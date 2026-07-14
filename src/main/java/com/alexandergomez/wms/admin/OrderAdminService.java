package com.alexandergomez.wms.admin;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexandergomez.wms.api.CorrelationIdFilter;
import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemException;
import com.alexandergomez.wms.catalog.Article;
import com.alexandergomez.wms.catalog.ArticleRepository;
import com.alexandergomez.wms.catalog.Location;
import com.alexandergomez.wms.catalog.LocationRepository;
import com.alexandergomez.wms.inventory.CandidateBin;
import com.alexandergomez.wms.inventory.Stock;
import com.alexandergomez.wms.inventory.StockJdbcRepository;
import com.alexandergomez.wms.inventory.StockRepository;
import com.alexandergomez.wms.orders.CustomerOrder;
import com.alexandergomez.wms.orders.CustomerOrderRepository;
import com.alexandergomez.wms.orders.OrderLine;
import com.alexandergomez.wms.orders.OrderLineRepository;
import com.alexandergomez.wms.picking.PickingTask;
import com.alexandergomez.wms.picking.PickingTaskRepository;
import com.alexandergomez.wms.picking.TaskStatus;
import com.alexandergomez.wms.picking.TaskTransition;
import com.alexandergomez.wms.picking.TaskTransitionRepository;

/**
 * Order creation with atomic multi-bin allocation, and order-detail reads
 * (API.md, ADR 0003). Allocation locks every candidate stock row in ascending
 * {@code (article_id, location_id)} order (deadlock-safety across concurrent
 * order-creation transactions and pick confirmations, which lock stock the
 * same way), recomputes availability under that lock, and either creates the
 * complete order/lines/tasks or creates nothing.
 */
@Service
public class OrderAdminService {

    private static final Logger log = LoggerFactory.getLogger(OrderAdminService.class);

    private final CustomerOrderRepository orders;
    private final OrderLineRepository orderLines;
    private final PickingTaskRepository pickingTasks;
    private final TaskTransitionRepository taskTransitions;
    private final ArticleRepository articles;
    private final LocationRepository locations;
    private final StockRepository stock;
    private final StockJdbcRepository stockJdbc;

    public OrderAdminService(CustomerOrderRepository orders, OrderLineRepository orderLines,
            PickingTaskRepository pickingTasks, TaskTransitionRepository taskTransitions,
            ArticleRepository articles, LocationRepository locations, StockRepository stock,
            StockJdbcRepository stockJdbc) {
        this.orders = orders;
        this.orderLines = orderLines;
        this.pickingTasks = pickingTasks;
        this.taskTransitions = taskTransitions;
        this.articles = articles;
        this.locations = locations;
        this.stock = stock;
        this.stockJdbc = stockJdbc;
    }

    @Transactional
    public CreateOrderResponse createOrder(Long adminUserId, CreateOrderRequest request) {
        if (orders.findByOrderNumber(request.orderNumber()).isPresent()) {
            throw new ProblemException(ProblemCode.ORDER_ALREADY_EXISTS,
                    "An order with this number already exists.");
        }
        requireDistinctLineNumbers(request);

        Map<Integer, Article> articleByLine = new LinkedHashMap<>();
        for (CreateOrderLineRequest line : request.lines()) {
            Article article = articles.findBySku(line.articleSku())
                    .orElseThrow(() -> new ProblemException(ProblemCode.ARTICLE_NOT_FOUND,
                            "Article not found: " + line.articleSku()));
            articleByLine.put(line.lineNumber(), article);
        }

        Map<Long, List<CandidateBin>> binsByArticle = new LinkedHashMap<>();
        for (Article article : new LinkedHashSet<>(articleByLine.values())) {
            binsByArticle.put(article.getId(), stockJdbc.candidateBinsForArticle(article.getId()));
        }

        List<BinKey> lockOrder = binsByArticle.values().stream()
                .flatMap(List::stream)
                .map(bin -> new BinKey(bin.articleId(), bin.locationId()))
                .distinct()
                .sorted(Comparator.comparing(BinKey::articleId).thenComparing(BinKey::locationId))
                .toList();
        Map<BinKey, Stock> locked = new LinkedHashMap<>();
        for (BinKey key : lockOrder) {
            Stock stockRow = stock.findByIdForUpdate(key.articleId(), key.locationId()).orElseThrow();
            locked.put(key, stockRow);
        }

        // One reservation snapshot per locked bin, taken once before the walk.
        // Two lines of the SAME order can reference the same article (and so
        // the same candidate bins); their combined draw is tracked in-memory
        // as the walk proceeds, because a not-yet-persisted planned task from
        // an earlier line in this same request is invisible to a fresh
        // unfinishedTaskReservation() query — without this, two lines could
        // each believe the full bin is available and double-allocate it.
        Map<BinKey, Integer> initialReservation = new LinkedHashMap<>();
        for (BinKey key : lockOrder) {
            initialReservation.put(key, stockJdbc.unfinishedTaskReservation(key.articleId(), key.locationId()));
        }
        Map<BinKey, Integer> consumedByThisOrder = new LinkedHashMap<>();

        List<PlannedTask> plannedTasks = new ArrayList<>();
        for (CreateOrderLineRequest line : request.lines()) {
            Article article = articleByLine.get(line.lineNumber());
            int remaining = line.quantity();
            int taskSequence = 1;
            for (CandidateBin bin : binsByArticle.get(article.getId())) {
                if (remaining <= 0) {
                    break;
                }
                BinKey key = new BinKey(bin.articleId(), bin.locationId());
                Stock stockRow = locked.get(key);
                int reserved = initialReservation.get(key) + consumedByThisOrder.getOrDefault(key, 0);
                int available = stockRow.getQuantity() - reserved;
                if (available <= 0) {
                    continue;
                }
                int draw = Math.min(remaining, available);
                plannedTasks.add(new PlannedTask(
                        line.lineNumber(), article.getId(), bin.locationId(), taskSequence, draw));
                consumedByThisOrder.merge(key, draw, Integer::sum);
                taskSequence++;
                remaining -= draw;
            }
            if (remaining > 0) {
                throw new ProblemException(ProblemCode.INSUFFICIENT_AVAILABLE_STOCK,
                        "Line " + line.lineNumber() + " cannot be fully allocated from available stock.");
            }
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        CustomerOrder order = orders.save(CustomerOrder.open(request.orderNumber(), adminUserId, now));

        Map<Integer, OrderLine> lineByNumber = new LinkedHashMap<>();
        for (CreateOrderLineRequest line : request.lines()) {
            OrderLine orderLine = OrderLine.create(
                    order.getId(), line.lineNumber(), articleByLine.get(line.lineNumber()).getId(), line.quantity());
            lineByNumber.put(line.lineNumber(), orderLines.save(orderLine));
        }

        UUID correlationId = currentCorrelationUuid();
        int taskCount = 0;
        for (PlannedTask planned : plannedTasks) {
            OrderLine orderLine = lineByNumber.get(planned.lineNumber());
            String taskNumber = String.format(
                    "%s-%03d-%02d", request.orderNumber(), planned.lineNumber(), planned.taskSequence());
            PickingTask task = pickingTasks.save(PickingTask.available(taskNumber, orderLine.getId(),
                    planned.taskSequence(), planned.articleId(), planned.locationId(), planned.quantity()));
            taskTransitions.save(TaskTransition.record(task.getId(), null, TaskStatus.AVAILABLE,
                    adminUserId, null, null, correlationId, null, now));
            taskCount++;
        }

        log.atInfo()
                .addKeyValue("orderNumber", order.getOrderNumber())
                .addKeyValue("lineCount", lineByNumber.size())
                .addKeyValue("taskCount", taskCount)
                .addKeyValue("adminUserId", adminUserId)
                .log("order created");

        return new CreateOrderResponse(order.getOrderNumber(), order.getStatus().name(),
                now.toInstant(), lineByNumber.size(), taskCount);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(String orderNumber) {
        CustomerOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ProblemException(ProblemCode.ORDER_NOT_FOUND, "Order not found."));

        List<OrderDetailResponse.LineDetail> lineDetails = new ArrayList<>();
        for (OrderLine line : orderLines.findByOrderIdOrderByLineNumber(order.getId())) {
            Article article = articles.findById(line.getArticleId()).orElseThrow();
            List<OrderDetailResponse.TaskDetail> taskDetails = new ArrayList<>();
            for (PickingTask task : pickingTasks.findByOrderLineIdOrderByTaskSequence(line.getId())) {
                Location location = locations.findById(task.getSourceLocationId()).orElseThrow();
                taskDetails.add(new OrderDetailResponse.TaskDetail(
                        task.getId(), location.getCode(), task.getRequestedQuantity(), task.getStatus().name()));
            }
            lineDetails.add(new OrderDetailResponse.LineDetail(line.getLineNumber(), article.getSku(),
                    line.getRequestedQuantity(), line.getPickedQuantity(), line.getStatus().name(), taskDetails));
        }

        return new OrderDetailResponse(order.getOrderNumber(), order.getStatus().name(),
                order.getCreatedAt().toInstant(),
                order.getCompletedAt() == null ? null : order.getCompletedAt().toInstant(),
                lineDetails);
    }

    private static void requireDistinctLineNumbers(CreateOrderRequest request) {
        long distinct = request.lines().stream().map(CreateOrderLineRequest::lineNumber).distinct().count();
        if (distinct != request.lines().size()) {
            throw new ProblemException(ProblemCode.VALIDATION_FAILED,
                    "Line numbers must be unique within an order.");
        }
    }

    private static UUID currentCorrelationUuid() {
        try {
            return UUID.fromString(CorrelationIdFilter.currentCorrelationId());
        } catch (IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }

    private record BinKey(long articleId, long locationId) {
    }

    private record PlannedTask(int lineNumber, long articleId, long locationId, int taskSequence, int quantity) {
    }
}

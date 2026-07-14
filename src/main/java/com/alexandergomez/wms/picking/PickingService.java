package com.alexandergomez.wms.picking;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexandergomez.wms.api.CorrelationIdFilter;
import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemException;
import com.alexandergomez.wms.catalog.Article;
import com.alexandergomez.wms.catalog.ArticleRepository;
import com.alexandergomez.wms.catalog.Location;
import com.alexandergomez.wms.catalog.LocationRepository;
import com.alexandergomez.wms.identity.AuthenticatedUser;
import com.alexandergomez.wms.inventory.Stock;
import com.alexandergomez.wms.inventory.StockId;
import com.alexandergomez.wms.inventory.StockMovement;
import com.alexandergomez.wms.inventory.StockMovementRepository;
import com.alexandergomez.wms.inventory.StockRepository;
import com.alexandergomez.wms.orders.CustomerOrder;
import com.alexandergomez.wms.orders.CustomerOrderRepository;
import com.alexandergomez.wms.orders.OrderLine;
import com.alexandergomez.wms.orders.OrderLineRepository;
import com.alexandergomez.wms.orders.OrderLineStatus;

/**
 * The picking happy-path workflow (ADR 0003, ADR 0004, API.md): atomic FIFO
 * claim, location/article scans, and the exact-quantity confirm transaction
 * that updates task, stock, movement, line, and order atomically.
 *
 * <p>Lock order is fixed across every mutating operation to avoid deadlocks:
 * task, then stock, then order line, then customer order. ADR 0003 mandates
 * task-then-stock explicitly; the order-line and customer-order locks are this
 * slice's addition, needed because a single order line can be split across
 * multiple tasks (multi-bin picking) that different pickers may confirm
 * concurrently, which would otherwise race on {@code picked_quantity}.
 */
@Service
public class PickingService {

    private static final Logger log = LoggerFactory.getLogger(PickingService.class);

    private static final List<TaskStatus> ACTIVE_STATES =
            List.of(TaskStatus.ASSIGNED, TaskStatus.LOCATION_CONFIRMED, TaskStatus.ARTICLE_CONFIRMED);

    private final PickingTaskRepository pickingTasks;
    private final PickingJdbcRepository pickingJdbc;
    private final TaskTransitionRepository taskTransitions;
    private final OrderLineRepository orderLines;
    private final CustomerOrderRepository orders;
    private final StockRepository stock;
    private final StockMovementRepository stockMovements;
    private final ArticleRepository articles;
    private final LocationRepository locations;

    public PickingService(PickingTaskRepository pickingTasks, PickingJdbcRepository pickingJdbc,
            TaskTransitionRepository taskTransitions, OrderLineRepository orderLines,
            CustomerOrderRepository orders, StockRepository stock, StockMovementRepository stockMovements,
            ArticleRepository articles, LocationRepository locations) {
        this.pickingTasks = pickingTasks;
        this.pickingJdbc = pickingJdbc;
        this.taskTransitions = taskTransitions;
        this.orderLines = orderLines;
        this.orders = orders;
        this.stock = stock;
        this.stockMovements = stockMovements;
        this.articles = articles;
        this.locations = locations;
    }

    @Transactional
    public Optional<NextTaskResponse> nextTask(AuthenticatedUser caller) {
        Optional<PickingTask> active =
                pickingTasks.findByAssignedUserIdAndStatusIn(caller.userId(), ACTIVE_STATES);
        if (active.isPresent()) {
            return Optional.of(toNextTaskResponse(active.get()));
        }

        Optional<Long> claimedId = pickingJdbc.claimNextAvailableTaskId();
        if (claimedId.isEmpty()) {
            return Optional.empty();
        }

        try {
            PickingTask task = pickingTasks.findByIdForUpdate(claimedId.get()).orElseThrow();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            TaskStatus previous = task.getStatus();
            task.assignTo(caller.userId(), caller.deviceId(), now);
            pickingTasks.saveAndFlush(task);

            OrderLine line = orderLines.findByIdForUpdate(task.getOrderLineId()).orElseThrow();
            line.markInProgress();
            orderLines.save(line);

            CustomerOrder order = orders.findByIdForUpdate(line.getOrderId()).orElseThrow();
            order.markInProgress();
            orders.save(order);

            taskTransitions.save(TaskTransition.record(task.getId(), previous, TaskStatus.ASSIGNED,
                    caller.userId(), caller.deviceId(), null, currentCorrelationUuid(), null, now));

            return Optional.of(toNextTaskResponse(task));
        } catch (DataIntegrityViolationException ex) {
            log.warn("task assignment conflict claimedTaskId={} userId={} deviceId={}",
                    claimedId.get(), caller.userId(), caller.deviceId());
            throw new ProblemException(ProblemCode.TASK_ASSIGNMENT_CONFLICT,
                    "Another request already claimed or assigned a task for this user/device.");
        }
    }

    @Transactional
    public ScanLocationResponse scanLocation(AuthenticatedUser caller, Long taskId, String qrValue) {
        PickingTask task = pickingTasks.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ProblemException(ProblemCode.TASK_NOT_FOUND, "Task not found."));
        requireOwnership(caller, task);

        Location location = locations.findById(task.getSourceLocationId()).orElseThrow();
        String expectedQr = location.getQrValue();

        if (task.getStatus() == TaskStatus.LOCATION_CONFIRMED && expectedQr.equals(qrValue)) {
            return new ScanLocationResponse(task.getId(), task.getStatus().name(), location.getCode(),
                    toInstant(task.getLocationConfirmedAt()), true);
        }
        if (task.getStatus() != TaskStatus.ASSIGNED) {
            throw new ProblemException(ProblemCode.INVALID_TASK_STATE, "Task is not awaiting a location scan.");
        }
        if (!expectedQr.equals(qrValue)) {
            throw new ProblemException(ProblemCode.WRONG_LOCATION,
                    "Scanned location does not match the assigned task.",
                    Map.of("taskId", task.getId(), "expectedLocationCode", location.getCode(),
                            "scannedQrValue", qrValue));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        TaskStatus previous = task.getStatus();
        task.confirmLocation(now);
        pickingTasks.save(task);
        taskTransitions.save(TaskTransition.record(task.getId(), previous, TaskStatus.LOCATION_CONFIRMED,
                caller.userId(), caller.deviceId(), null, currentCorrelationUuid(), null, now));

        return new ScanLocationResponse(task.getId(), task.getStatus().name(), location.getCode(),
                toInstant(now), false);
    }

    @Transactional
    public ScanArticleResponse scanArticle(AuthenticatedUser caller, Long taskId, String qrValue) {
        PickingTask task = pickingTasks.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ProblemException(ProblemCode.TASK_NOT_FOUND, "Task not found."));
        requireOwnership(caller, task);

        Article article = articles.findById(task.getArticleId()).orElseThrow();
        String expectedQr = article.getQrValue();

        if (task.getStatus() == TaskStatus.ARTICLE_CONFIRMED && expectedQr.equals(qrValue)) {
            return new ScanArticleResponse(task.getId(), task.getStatus().name(), article.getSku(),
                    toInstant(task.getArticleConfirmedAt()), true);
        }
        if (task.getStatus() != TaskStatus.LOCATION_CONFIRMED) {
            throw new ProblemException(ProblemCode.INVALID_TASK_STATE, "Task is not awaiting an article scan.");
        }
        if (!expectedQr.equals(qrValue)) {
            throw new ProblemException(ProblemCode.WRONG_ARTICLE,
                    "Scanned article does not match the assigned task.",
                    Map.of("taskId", task.getId(), "expectedArticleSku", article.getSku(),
                            "scannedQrValue", qrValue));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        TaskStatus previous = task.getStatus();
        task.confirmArticle(now);
        pickingTasks.save(task);
        taskTransitions.save(TaskTransition.record(task.getId(), previous, TaskStatus.ARTICLE_CONFIRMED,
                caller.userId(), caller.deviceId(), null, currentCorrelationUuid(), null, now));

        return new ScanArticleResponse(task.getId(), task.getStatus().name(), article.getSku(),
                toInstant(now), false);
    }

    @Transactional
    public ConfirmResponse confirm(AuthenticatedUser caller, Long taskId, UUID confirmationId, int quantity) {
        PickingTask task = pickingTasks.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ProblemException(ProblemCode.TASK_NOT_FOUND, "Task not found."));
        requireOwnership(caller, task);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            if (confirmationId.equals(task.getConfirmationId())) {
                if (quantity == task.getConfirmedQuantity()) {
                    return toConfirmResponse(task);
                }
                throw new ProblemException(ProblemCode.CONFIRMATION_ID_REUSED,
                        "The confirmation ID was already used with a different quantity.");
            }
            throw new ProblemException(ProblemCode.INVALID_TASK_STATE, "Task is already completed.");
        }
        if (task.getStatus() != TaskStatus.ARTICLE_CONFIRMED) {
            throw new ProblemException(ProblemCode.INVALID_TASK_STATE, "Task is not ready for confirmation.");
        }
        if (quantity != task.getRequestedQuantity()) {
            throw new ProblemException(ProblemCode.QUANTITY_MISMATCH,
                    "Confirmed quantity must equal the task's requested quantity.");
        }

        try {
            Stock stockRow = stock.findByIdForUpdate(task.getArticleId(), task.getSourceLocationId())
                    .orElseThrow(() -> new IllegalStateException("Stock row missing for a valid task"));
            if (stockRow.getQuantity() < quantity) {
                throw new ProblemException(ProblemCode.INSUFFICIENT_STOCK,
                        "Not enough stock remains at this location.");
            }

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            TaskStatus previous = task.getStatus();
            task.complete(quantity, confirmationId, now);
            pickingTasks.saveAndFlush(task);

            // Flushed immediately: the trigger backing "stock updates before the
            // movement is inserted" (architecture.md) validates the movement's
            // resultingQuantity against the row's *current* committed value, and
            // the movement insert below executes right away (IDENTITY strategy).
            stockRow.decrease(quantity, now);
            stock.saveAndFlush(stockRow);

            OrderLine line = orderLines.findByIdForUpdate(task.getOrderLineId()).orElseThrow();

            UUID correlationId = currentCorrelationUuid();
            StockMovement movement = StockMovement.pick(task.getArticleId(), task.getSourceLocationId(), quantity,
                    stockRow.getQuantity(), line.getOrderId(), line.getId(), task.getId(),
                    caller.userId(), caller.deviceId(), "HHT pick confirmation", correlationId, now);
            movement = stockMovements.save(movement);

            taskTransitions.save(TaskTransition.record(task.getId(), previous, TaskStatus.COMPLETED,
                    caller.userId(), caller.deviceId(), null, correlationId, confirmationId, now));

            line.addPickedQuantity(quantity);
            long remainingTasksOnLine =
                    pickingTasks.countByOrderLineIdAndStatusNot(line.getId(), TaskStatus.COMPLETED);
            if (remainingTasksOnLine == 0) {
                line.markCompleted();
            }
            orderLines.save(line);

            CustomerOrder order = orders.findByIdForUpdate(line.getOrderId()).orElseThrow();
            long remainingLinesOnOrder =
                    orderLines.countByOrderIdAndStatusNot(order.getId(), OrderLineStatus.COMPLETED);
            if (remainingLinesOnOrder == 0) {
                order.markCompleted(now);
            }
            orders.save(order);

            return new ConfirmResponse(task.getId(), task.getStatus().name(), task.getConfirmedQuantity(),
                    movement.getId(), stockRow.getQuantity(),
                    new ConfirmResponse.OrderSummary(order.getOrderNumber(), order.getStatus().name()),
                    toInstant(task.getCompletedAt()));
        } catch (DataIntegrityViolationException ex) {
            throw new ProblemException(ProblemCode.CONFIRMATION_ID_REUSED,
                    "The confirmation ID conflicts with an existing confirmation.");
        }
    }

    private ConfirmResponse toConfirmResponse(PickingTask task) {
        StockMovement movement = stockMovements.findByPickingTaskId(task.getId())
                .orElseThrow(() -> new IllegalStateException("Completed task missing its movement"));
        Stock stockRow = stock.findById(new StockId(task.getArticleId(), task.getSourceLocationId())).orElseThrow();
        OrderLine line = orderLines.findById(task.getOrderLineId()).orElseThrow();
        CustomerOrder order = orders.findById(line.getOrderId()).orElseThrow();
        return new ConfirmResponse(task.getId(), task.getStatus().name(), task.getConfirmedQuantity(),
                movement.getId(), stockRow.getQuantity(),
                new ConfirmResponse.OrderSummary(order.getOrderNumber(), order.getStatus().name()),
                toInstant(task.getCompletedAt()));
    }

    private NextTaskResponse toNextTaskResponse(PickingTask task) {
        OrderLine line = orderLines.findById(task.getOrderLineId()).orElseThrow();
        CustomerOrder order = orders.findById(line.getOrderId()).orElseThrow();
        Article article = articles.findById(task.getArticleId()).orElseThrow();
        Location location = locations.findById(task.getSourceLocationId()).orElseThrow();
        return new NextTaskResponse(task.getId(), task.getStatus().name(), order.getOrderNumber(),
                line.getLineNumber(), task.getTaskSequence(),
                new NextTaskResponse.LocationSummary(location.getCode()),
                new NextTaskResponse.ArticleSummary(article.getSku(), article.getDescription()),
                task.getRequestedQuantity(), toInstant(task.getAssignedAt()));
    }

    private static void requireOwnership(AuthenticatedUser caller, PickingTask task) {
        if (!caller.userId().equals(task.getAssignedUserId())
                || !caller.deviceId().equals(task.getAssignedDeviceId())) {
            throw new ProblemException(ProblemCode.TASK_NOT_ASSIGNED_TO_USER,
                    "This task is not assigned to the caller.");
        }
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private static UUID currentCorrelationUuid() {
        try {
            return UUID.fromString(CorrelationIdFilter.currentCorrelationId());
        } catch (IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }
}

# SQL diagnostic query pack

Run these queries against the same database used by the WMS application. They are read-only and safe during normal operation. Record the query, UTC execution time, active Spring profile, build version, and correlation/order/task identifiers in incident evidence.

Example interactive connection from the project directory:

```powershell
docker compose exec postgres psql -U wms -d wms
```

## 1. Find stuck picking tasks

Purpose: identify assigned work whose state has not advanced within the operational threshold. The query uses 30 minutes to match the development default. Change the interval to the configured `wms.task.stuck-threshold` value when investigating another environment.

```sql
SELECT
    t.id AS task_id,
    t.task_number,
    o.order_number,
    ol.line_number,
    t.status AS task_status,
    u.username AS assigned_user,
    d.device_code,
    l.code AS location_code,
    a.sku,
    t.requested_quantity,
    t.assigned_at,
    t.last_transition_at,
    CURRENT_TIMESTAMP - t.last_transition_at AS time_in_current_state
FROM picking_task t
JOIN order_line ol ON ol.id = t.order_line_id
JOIN customer_order o ON o.id = ol.order_id
JOIN article a ON a.id = t.article_id
JOIN location l ON l.id = t.source_location_id
LEFT JOIN app_user u ON u.id = t.assigned_user_id
LEFT JOIN device d ON d.id = t.assigned_device_id
WHERE t.status IN ('ASSIGNED', 'LOCATION_CONFIRMED', 'ARTICLE_CONFIRMED')
  AND t.last_transition_at < CURRENT_TIMESTAMP - INTERVAL '30 minutes'
ORDER BY t.last_transition_at, o.created_at, ol.line_number, t.task_sequence;
```

Expected seed result: `DEMO-1002-001-01`, in `LOCATION_CONFIRMED`, is deliberately older than the threshold. An empty result in normal operation means no active task is stuck.

Useful follow-up checks:

```sql
SELECT *
FROM auth_token
WHERE user_id = (SELECT assigned_user_id FROM picking_task WHERE task_number = 'DEMO-1002-001-01')
ORDER BY created_at DESC;

SELECT id, movement_type, quantity_delta, resulting_quantity, correlation_id, occurred_at
FROM stock_movement
WHERE picking_task_id = (SELECT id FROM picking_task WHERE task_number = 'DEMO-1002-001-01');
```

Do not repair a task with direct SQL. Capture evidence and use the future administration recovery operation so that state changes remain logged and tested.

## 2. Find stock discrepancies against movements

Purpose: reconcile each physical stock row with the sum of its append-only ledger. Every stock-changing transaction must update `stock` and insert one `stock_movement`; therefore the two quantities must agree.

```sql
WITH ledger AS (
    SELECT
        article_id,
        location_id,
        SUM(quantity_delta) AS ledger_quantity,
        MAX(occurred_at) AS last_movement_at
    FROM stock_movement
    GROUP BY article_id, location_id
),
reconciliation AS (
    SELECT
        COALESCE(s.article_id, ledger.article_id) AS article_id,
        COALESCE(s.location_id, ledger.location_id) AS location_id,
        s.quantity AS stock_quantity,
        COALESCE(ledger.ledger_quantity, 0) AS ledger_quantity,
        COALESCE(s.quantity, 0) - COALESCE(ledger.ledger_quantity, 0) AS difference,
        ledger.last_movement_at
    FROM stock s
    FULL OUTER JOIN ledger
      ON ledger.article_id = s.article_id
     AND ledger.location_id = s.location_id
)
SELECT
    a.sku,
    l.code AS location_code,
    r.stock_quantity,
    r.ledger_quantity,
    r.difference,
    r.last_movement_at
FROM reconciliation r
JOIN article a ON a.id = r.article_id
JOIN location l ON l.id = r.location_id
WHERE r.stock_quantity IS NULL
   OR r.stock_quantity <> r.ledger_quantity
ORDER BY a.sku, l.code;
```

Expected seed result: zero rows. Any row is an incident. Preserve database and application logs before considering recovery.

To inspect the affected ledger in exact order:

```sql
SELECT
    m.id,
    m.movement_type,
    m.quantity_delta,
    m.resulting_quantity,
    m.reason,
    m.correlation_id,
    m.occurred_at,
    o.order_number,
    t.task_number,
    u.username,
    d.device_code
FROM stock_movement m
LEFT JOIN customer_order o ON o.id = m.order_id
LEFT JOIN picking_task t ON t.id = m.picking_task_id
LEFT JOIN app_user u ON u.id = m.performed_by_user_id
LEFT JOIN device d ON d.id = m.device_id
WHERE m.article_id = (SELECT id FROM article WHERE sku = 'ART-002')
  AND m.location_id = (SELECT id FROM location WHERE code = 'A-02-01')
ORDER BY m.occurred_at, m.id;
```

Check that every row's `resulting_quantity` equals the running total:

```sql
SELECT
    m.id,
    m.occurred_at,
    m.quantity_delta,
    m.resulting_quantity,
    SUM(m.quantity_delta) OVER (
        PARTITION BY m.article_id, m.location_id
        ORDER BY m.occurred_at, m.id
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS calculated_result,
    m.resulting_quantity - SUM(m.quantity_delta) OVER (
        PARTITION BY m.article_id, m.location_id
        ORDER BY m.occurred_at, m.id
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS row_difference
FROM stock_movement m
WHERE m.article_id = (SELECT id FROM article WHERE sku = 'ART-002')
  AND m.location_id = (SELECT id FROM location WHERE code = 'A-02-01')
ORDER BY m.occurred_at, m.id;
```

## 3. Trace one order end to end (current-state reconstruction)

Purpose: combine order creation, current line/task state, and immutable stock movements in one evidence stream. Change only the value in `params`. Phase 1 does not store every intermediate scan transition, so the task row is a current/final-state snapshot rather than a complete transition history; phase 4 structured logs provide those intermediate events.

```sql
WITH params AS (
    SELECT 'DEMO-1003'::VARCHAR AS order_number
),
order_events AS (
    SELECT
        o.created_at AS event_time,
        1 AS event_sequence,
        'ORDER_CREATED'::TEXT AS event_type,
        o.order_number,
        NULL::INTEGER AS line_number,
        NULL::TEXT AS task_number,
        NULL::BIGINT AS movement_id,
        jsonb_build_object(
            'status', o.status,
            'createdBy', creator.username,
            'releasedAt', o.released_at,
            'completedAt', o.completed_at
        ) AS details
    FROM customer_order o
    JOIN app_user creator ON creator.id = o.created_by_user_id
    JOIN params p ON p.order_number = o.order_number
),
line_events AS (
    SELECT
        ol.created_at AS event_time,
        2 AS event_sequence,
        'LINE_CREATED'::TEXT AS event_type,
        o.order_number,
        ol.line_number,
        NULL::TEXT AS task_number,
        NULL::BIGINT AS movement_id,
        jsonb_build_object(
            'sku', a.sku,
            'status', ol.status,
            'requestedQuantity', ol.requested_quantity,
            'pickedQuantity', ol.picked_quantity
        ) AS details
    FROM order_line ol
    JOIN customer_order o ON o.id = ol.order_id
    JOIN article a ON a.id = ol.article_id
    JOIN params p ON p.order_number = o.order_number
),
task_events AS (
    SELECT
        COALESCE(t.completed_at, t.last_transition_at, t.created_at) AS event_time,
        3 AS event_sequence,
        'TASK_' || t.status AS event_type,
        o.order_number,
        ol.line_number,
        t.task_number,
        NULL::BIGINT AS movement_id,
        jsonb_build_object(
            'location', l.code,
            'sku', a.sku,
            'requestedQuantity', t.requested_quantity,
            'confirmedQuantity', t.confirmed_quantity,
            'assignedUser', u.username,
            'device', d.device_code,
            'assignedAt', t.assigned_at,
            'completedAt', t.completed_at,
            'confirmationId', t.confirmation_id
        ) AS details
    FROM picking_task t
    JOIN order_line ol ON ol.id = t.order_line_id
    JOIN customer_order o ON o.id = ol.order_id
    JOIN article a ON a.id = t.article_id
    JOIN location l ON l.id = t.source_location_id
    LEFT JOIN app_user u ON u.id = t.assigned_user_id
    LEFT JOIN device d ON d.id = t.assigned_device_id
    JOIN params p ON p.order_number = o.order_number
),
movement_events AS (
    SELECT
        m.occurred_at AS event_time,
        4 AS event_sequence,
        'MOVEMENT_' || m.movement_type AS event_type,
        o.order_number,
        ol.line_number,
        t.task_number,
        m.id AS movement_id,
        jsonb_build_object(
            'location', l.code,
            'sku', a.sku,
            'quantityDelta', m.quantity_delta,
            'resultingQuantity', m.resulting_quantity,
            'reason', m.reason,
            'correlationId', m.correlation_id,
            'user', u.username,
            'device', d.device_code
        ) AS details
    FROM stock_movement m
    JOIN customer_order o ON o.id = m.order_id
    JOIN order_line ol ON ol.id = m.order_line_id
    JOIN picking_task t ON t.id = m.picking_task_id
    JOIN article a ON a.id = m.article_id
    JOIN location l ON l.id = m.location_id
    LEFT JOIN app_user u ON u.id = m.performed_by_user_id
    LEFT JOIN device d ON d.id = m.device_id
    JOIN params p ON p.order_number = o.order_number
)
SELECT event_time, event_type, order_number, line_number, task_number, movement_id, details
FROM (
    SELECT * FROM order_events
    UNION ALL
    SELECT * FROM line_events
    UNION ALL
    SELECT * FROM task_events
    UNION ALL
    SELECT * FROM movement_events
) trace
ORDER BY event_time, event_sequence, line_number NULLS FIRST, task_number NULLS FIRST, movement_id NULLS FIRST;
```

Expected seed result for `DEMO-1003`: one completed order, one picked line, one completed task, and one `PICK` movement for `-2` units. The movement's `correlationId`, task, user, and device are the primary joins back to structured logs.

## 4. Integrity overview for shift handover

This compact read-only check is useful before and after a test run:

```sql
SELECT 'active_tasks' AS check_name, count(*)::BIGINT AS value
FROM picking_task
WHERE status IN ('ASSIGNED', 'LOCATION_CONFIRMED', 'ARTICLE_CONFIRMED')
UNION ALL
SELECT 'stuck_tasks', count(*)
FROM picking_task
WHERE status IN ('ASSIGNED', 'LOCATION_CONFIRMED', 'ARTICLE_CONFIRMED')
  AND last_transition_at < CURRENT_TIMESTAMP - INTERVAL '30 minutes'
UNION ALL
SELECT 'completed_orders', count(*)
FROM customer_order
WHERE status = 'COMPLETED'
UNION ALL
SELECT 'stock_discrepancies', count(*)
FROM stock s
LEFT JOIN (
    SELECT article_id, location_id, sum(quantity_delta) AS quantity
    FROM stock_movement
    GROUP BY article_id, location_id
) m USING (article_id, location_id)
WHERE s.quantity <> COALESCE(m.quantity, 0);
```

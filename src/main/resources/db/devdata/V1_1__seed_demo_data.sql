-- Development/demo credentials only. This location is excluded from preprod.
-- admin / admin123, picker01 / picker123
INSERT INTO app_user (username, password_hash, role)
VALUES
    ('admin', crypt('admin123', gen_salt('bf', 10)), 'ADMIN'),
    ('picker01', crypt('picker123', gen_salt('bf', 10)), 'PICKER');

INSERT INTO device (device_code, description)
VALUES ('HHT-PI-01', 'Raspberry Pi handheld scanner 01');

INSERT INTO article (sku, description, qr_value)
VALUES
    ('ART-001', 'Black basic T-shirt', 'ART:ART-001'),
    ('ART-002', 'Blue straight-fit jeans', 'ART:ART-002'),
    ('ART-003', 'White low-top trainers', 'ART:ART-003'),
    ('ART-004', 'Natural canvas tote bag', 'ART:ART-004');

INSERT INTO location (code, qr_value, pick_sequence)
VALUES
    ('A-01-01', 'LOC:A-01-01', 10101),
    ('A-01-02', 'LOC:A-01-02', 10102),
    ('A-02-01', 'LOC:A-02-01', 10201),
    ('B-01-01', 'LOC:B-01-01', 20101),
    ('B-01-02', 'LOC:B-01-02', 20102);

INSERT INTO stock (article_id, location_id, quantity)
SELECT a.id, l.id, seed.quantity
FROM (
    VALUES
        ('ART-001', 'A-01-01', 20),
        ('ART-001', 'A-01-02', 10),
        ('ART-002', 'A-02-01', 15),
        ('ART-003', 'B-01-01', 8),
        ('ART-004', 'B-01-02', 12)
) AS seed(sku, location_code, quantity)
JOIN article a ON a.sku = seed.sku
JOIN location l ON l.code = seed.location_code;

INSERT INTO stock_movement (
    movement_type, article_id, location_id, quantity_delta, resulting_quantity,
    performed_by_user_id, reason, occurred_at
)
SELECT
    'INITIAL_STOCK', s.article_id, s.location_id, s.quantity, s.quantity,
    u.id, 'Flyway demonstration seed', CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM stock s
CROSS JOIN app_user u
WHERE u.username = 'admin';

INSERT INTO customer_order (
    order_number, status, created_by_user_id, created_at, released_at
)
SELECT seed.order_number, seed.status, u.id, seed.created_at, seed.released_at
FROM (
    VALUES
        ('DEMO-1001', 'RELEASED', CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '29 minutes'),
        ('DEMO-1002', 'PICKING', CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP - INTERVAL '3 hours 59 minutes')
) AS seed(order_number, status, created_at, released_at)
CROSS JOIN app_user u
WHERE u.username = 'admin';

INSERT INTO customer_order (
    order_number, status, created_by_user_id, created_at, released_at, completed_at
)
SELECT
    'DEMO-1003', 'COMPLETED', u.id,
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP - INTERVAL '23 hours 59 minutes',
    CURRENT_TIMESTAMP - INTERVAL '23 hours'
FROM app_user u
WHERE u.username = 'admin';

INSERT INTO order_line (
    order_id, line_number, article_id, requested_quantity, picked_quantity, status, created_at
)
SELECT o.id, seed.line_number, a.id, seed.requested_quantity, seed.picked_quantity, seed.status, o.created_at
FROM (
    VALUES
        ('DEMO-1001', 1, 'ART-001', 25, 0, 'ALLOCATED'),
        ('DEMO-1001', 2, 'ART-003', 2, 0, 'ALLOCATED'),
        ('DEMO-1002', 1, 'ART-004', 3, 0, 'PICKING'),
        ('DEMO-1003', 1, 'ART-002', 2, 2, 'PICKED')
) AS seed(order_number, line_number, sku, requested_quantity, picked_quantity, status)
JOIN customer_order o ON o.order_number = seed.order_number
JOIN article a ON a.sku = seed.sku;

INSERT INTO picking_task (
    task_number, order_line_id, task_sequence, article_id, source_location_id,
    requested_quantity, status, created_at, last_transition_at
)
SELECT
    seed.task_number, ol.id, seed.task_sequence, ol.article_id, l.id,
    seed.requested_quantity, 'AVAILABLE', o.created_at, o.released_at
FROM (
    VALUES
        ('DEMO-1001-001-01', 'DEMO-1001', 1, 1, 'A-01-01', 20),
        ('DEMO-1001-001-02', 'DEMO-1001', 1, 2, 'A-01-02', 5),
        ('DEMO-1001-002-01', 'DEMO-1001', 2, 1, 'B-01-01', 2)
) AS seed(task_number, order_number, line_number, task_sequence, location_code, requested_quantity)
JOIN customer_order o ON o.order_number = seed.order_number
JOIN order_line ol ON ol.order_id = o.id AND ol.line_number = seed.line_number
JOIN location l ON l.code = seed.location_code;

INSERT INTO picking_task (
    task_number, order_line_id, task_sequence, article_id, source_location_id,
    requested_quantity, status, assigned_user_id, assigned_device_id,
    created_at, assigned_at, last_transition_at
)
SELECT
    'DEMO-1002-001-01', ol.id, 1, ol.article_id, l.id,
    3, 'LOCATION_CONFIRMED', u.id, d.id,
    o.created_at, CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'
FROM customer_order o
JOIN order_line ol ON ol.order_id = o.id AND ol.line_number = 1
JOIN location l ON l.code = 'B-01-02'
JOIN app_user u ON u.username = 'picker01'
JOIN device d ON d.device_code = 'HHT-PI-01'
WHERE o.order_number = 'DEMO-1002';

INSERT INTO picking_task (
    task_number, order_line_id, task_sequence, article_id, source_location_id,
    requested_quantity, confirmed_quantity, status, assigned_user_id, assigned_device_id,
    confirmation_id, created_at, assigned_at, last_transition_at, completed_at
)
SELECT
    'DEMO-1003-001-01', ol.id, 1, ol.article_id, l.id,
    2, 2, 'COMPLETED', u.id, d.id,
    '2cfdb06f-0cd2-4ea5-995c-c167e3e391c4'::UUID,
    o.created_at, o.released_at, o.completed_at, o.completed_at
FROM customer_order o
JOIN order_line ol ON ol.order_id = o.id AND ol.line_number = 1
JOIN location l ON l.code = 'A-02-01'
JOIN app_user u ON u.username = 'picker01'
JOIN device d ON d.device_code = 'HHT-PI-01'
WHERE o.order_number = 'DEMO-1003';

UPDATE stock s
SET quantity = s.quantity - 2,
    version = s.version + 1,
    updated_at = CURRENT_TIMESTAMP - INTERVAL '23 hours'
FROM article a, location l
WHERE s.article_id = a.id
  AND s.location_id = l.id
  AND a.sku = 'ART-002'
  AND l.code = 'A-02-01';

INSERT INTO stock_movement (
    movement_type, article_id, location_id, quantity_delta, resulting_quantity,
    order_id, order_line_id, picking_task_id, performed_by_user_id, device_id,
    reason, correlation_id, occurred_at
)
SELECT
    'PICK', t.article_id, t.source_location_id, -t.confirmed_quantity, s.quantity,
    o.id, ol.id, t.id, t.assigned_user_id, t.assigned_device_id,
    'Completed demonstration pick',
    '8207c66c-6ee3-47d2-8329-0e94ff654b2a'::UUID,
    t.completed_at
FROM picking_task t
JOIN order_line ol ON ol.id = t.order_line_id
JOIN customer_order o ON o.id = ol.order_id
JOIN stock s ON s.article_id = t.article_id AND s.location_id = t.source_location_id
WHERE o.order_number = 'DEMO-1003';

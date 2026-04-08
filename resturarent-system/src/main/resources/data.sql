-- Seed data for the orders and order_items tables

INSERT INTO orders (customer_name, table_number, guest_count, status, total, created_at, updated_at)
VALUES ('Isuru', 'Table 14', 4, 'PAID',   840.00, '2026-02-14 10:45:00', '2026-02-14 10:50:00');
INSERT INTO orders (customer_name, table_number, guest_count, status, total, created_at, updated_at)
VALUES ('Kavinda', 'Table 02', 2, 'OPEN',   320.50, '2026-02-14 11:02:00', '2026-02-14 11:02:00');
INSERT INTO orders (customer_name, table_number, guest_count, status, total, created_at, updated_at)
VALUES ('Nimal', 'Table 09', 6, 'OPEN',  1120.00, '2026-02-14 11:15:00', '2026-02-14 11:15:00');
INSERT INTO orders (customer_name, table_number, guest_count, status, total, created_at, updated_at)
VALUES ('Suresh', 'Table 22', 3, 'CLOSED',  450.00, '2026-02-14 09:30:00', '2026-02-14 09:45:00');
INSERT INTO orders (customer_name, table_number, guest_count, status, total, created_at, updated_at)
VALUES ('Dinesh', 'Table 05', 2, 'CANCELLED', 0.00, '2026-02-14 09:15:00', '2026-02-14 09:20:00');
INSERT INTO orders (customer_name, table_number, guest_count, status, total, created_at, updated_at)
VALUES ('Amara', 'Table 11', 4, 'OPEN',   680.00, '2026-02-14 08:45:00', '2026-02-14 08:45:00');
INSERT INTO orders (customer_name, table_number, guest_count, status, total, created_at, updated_at)
VALUES ('Ruwan', 'Table 07', 5, 'PAID',   950.00, '2026-02-14 08:30:00', '2026-02-14 08:40:00');
INSERT INTO orders (customer_name, table_number, guest_count, status, total, created_at, updated_at)
VALUES ('Chamara', 'Table 03', 2, 'OPEN',   410.00, '2026-02-14 07:50:00', '2026-02-14 07:50:00');

-- Items for Order 1 (Table 14 - PAID)
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (1, 'Chicken Biryani', 2, 350.00, 'Extra spicy');
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (1, 'Lime Juice', 2, 70.00, NULL);

-- Items for Order 2 (Table 02 - OPEN)
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (2, 'Fried Rice', 1, 250.50, 'No prawns');
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (2, 'Coca Cola', 1, 70.00, NULL);

-- Items for Order 3 (Table 09 - OPEN)
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (3, 'Seafood Platter', 1, 850.00, 'Sauce on side');
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (3, 'Garlic Bread', 2, 135.00, NULL);

-- Items for Order 4 (Table 22 - CLOSED)
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (4, 'Pasta Carbonara', 1, 320.00, NULL);
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (4, 'Fresh Juice', 1, 130.00, NULL);

-- Items for Order 5 (Table 05 - CANCELLED)
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (5, 'Grilled Chicken', 1, 450.00, NULL);

-- Items for Order 6 (Table 11 - OPEN)
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (6, 'Kottu Roti', 2, 280.00, 'Extra cheese');
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (6, 'Ice Tea', 2, 60.00, NULL);

-- Items for Order 7 (Table 07 - PAID)
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (7, 'Lamb Curry', 2, 400.00, NULL);
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (7, 'Naan Bread', 3, 50.00, NULL);

-- Items for Order 8 (Table 03 - OPEN)
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (8, 'String Hoppers', 10, 30.00, NULL);
INSERT INTO order_items (order_id, item_name, quantity, unit_price, kitchen_notes)
VALUES (8, 'Dhal Curry', 1, 110.00, 'Extra salt');

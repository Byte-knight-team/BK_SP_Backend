-- Base RBAC and user/customer records
INSERT INTO roles (id, name, description)
VALUES (1, 'ROLE_CUSTOMER', 'Customer role')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO users (id, username, password, email, phone, address, role_id, is_active, created_at, updated_at)
VALUES
	(1, 'isuru', '$2a$10$7QJQnD3yFf8fE8aQfQJvJ.xnP7m6rA3m0QfD1m9q2dB8oH4T2uVbS', 'isuru@example.com', '0711111111', 'Colombo 01', 1, true, '2026-02-14 07:00:00', '2026-02-14 07:00:00'),
	(2, 'kavinda', '$2a$10$7QJQnD3yFf8fE8aQfQJvJ.xnP7m6rA3m0QfD1m9q2dB8oH4T2uVbS', 'kavinda@example.com', '0722222222', 'Colombo 02', 1, true, '2026-02-14 07:00:00', '2026-02-14 07:00:00');

INSERT INTO customers (id, user_id, loyalty_points, total_spent, email_verified, phone_verified)
VALUES
	(1, 1, 120, 840.00, true, true),
	(2, 2, 60, 410.00, true, true);

-- Branch and table data
INSERT INTO branches (id, name, address, contact_number, email, status, created_at)
VALUES (1, 'Main Branch', '123 Food St', '0112345678', 'main@kitchen.com', 'ACTIVE', '2026-02-14 06:30:00');

INSERT INTO restaurant_tables (id, branch_id, table_number, capacity, state, current_guest_count, active_order_count, created_at)
VALUES
	(1, 1, 14, 4, 'OCCUPIED', 4, 1, '2026-02-14 06:40:00'),
	(2, 1, 2, 2, 'AVAILABLE', 0, 0, '2026-02-14 06:40:00');

-- Category and menu seed
INSERT INTO categories (id, name, description)
VALUES
	(1, 'Main Course', 'Primary meal items'),
	(2, 'Beverages', 'Drinks and refreshments');

INSERT INTO menu_items (id, branch_id, category_id, sub_category, name, description, price, image_url, is_available, status, preparation_time, created_at)
VALUES
	(1, 1, 1, 'Rice', 'Chicken Biryani', 'Spiced basmati rice with chicken', 350.00, NULL, true, 'APPROVED', 20, '2026-02-14 06:45:00'),
	(2, 1, 2, 'Cold Drinks', 'Lime Juice', 'Fresh lime with ice', 70.00, NULL, true, 'APPROVED', 5, '2026-02-14 06:45:00');

-- Payment methods
INSERT INTO payment_methods (id, name, description, status, created_at)
VALUES
	(1, 'CARD', 'Card payment', 'ACTIVE', '2026-02-14 06:50:00'),
	(2, 'CASH', 'Cash payment', 'ACTIVE', '2026-02-14 06:50:00');

-- Orders with required customer_id (no guest QR orders)
INSERT INTO orders (id, order_number, branch_id, table_id, customer_id, order_type, status, total_amount, discount_amount, final_amount, payment_status, created_at, updated_at)
VALUES
	(1, 'ORD-1204', 1, 1, 1, 'QR', 'PLACED', 420.00, 0.00, 420.00, 'PENDING', '2026-02-14 10:45:00', '2026-02-14 10:50:00'),
	(2, 'ORD-1205', 1, 2, 2, 'ONLINE', 'APPROVED', 140.00, 0.00, 140.00, 'PAID', '2026-02-14 11:02:00', '2026-02-14 11:05:00');

INSERT INTO order_items (id, order_id, menu_item_id, item_name, quantity, unit_price, subtotal, kitchen_notes)
VALUES
	(1, 1, 1, 'Chicken Biryani', 1, 350.00, 350.00, 'Extra spicy'),
	(2, 1, 2, 'Lime Juice', 1, 70.00, 70.00, NULL),
	(3, 2, 2, 'Lime Juice', 2, 70.00, 140.00, NULL);

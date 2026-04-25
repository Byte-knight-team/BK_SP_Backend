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
;

-- Seed a default branch so tables can be created
INSERT IGNORE INTO branches (id, name, address, status, created_at)
VALUES (1, 'Main Branch', '123 Main Street', 'ACTIVE', NOW());

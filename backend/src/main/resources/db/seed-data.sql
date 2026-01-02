-- SVIMS Seed Data
-- Sample data for testing and development
-- PostgreSQL Version

-- Insert Roles
INSERT INTO roles (name) VALUES 
('ROLE_ADMIN'),
('ROLE_MANAGER'),
('ROLE_FINANCE')
ON CONFLICT (name) DO NOTHING;

-- Insert Users (password: password123 - BCrypt encoded)
-- Admin user
INSERT INTO users (username, email, password, is_active) VALUES 
('admin', 'admin@svims.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ5C', TRUE),
('manager1', 'manager1@svims.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ5C', TRUE),
('finance1', 'finance1@svims.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ5C', TRUE)
ON CONFLICT (username) DO NOTHING;

-- Assign Roles to Users
INSERT INTO user_roles (user_id, role_id) VALUES 
((SELECT id FROM users WHERE username='admin'), (SELECT id FROM roles WHERE name='ROLE_ADMIN')),
((SELECT id FROM users WHERE username='manager1'), (SELECT id FROM roles WHERE name='ROLE_MANAGER')),
((SELECT id FROM users WHERE username='finance1'), (SELECT id FROM roles WHERE name='ROLE_FINANCE'))
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Insert Vendors
INSERT INTO vendors (name, gstin, email, status, risk_score) VALUES 
('ABC Suppliers Pvt Ltd', '27AABCU1234F1Z5', 'abc@suppliers.com', 'ACTIVE', 15.5),
('XYZ Trading Company', '29BXYZU5678G2Z6', 'xyz@trading.com', 'ACTIVE', 8.2),
('Tech Solutions Inc', '09CTECHU9012H3Z7', 'tech@solutions.com', 'ACTIVE', 25.8),
('Global Imports Ltd', '19DGLOBU3456I4Z8', 'global@imports.com', 'ACTIVE', 5.0),
('Prime Distributors', '24EPRIMU7890J5Z9', 'prime@distributors.com', 'INACTIVE', 45.3)
ON CONFLICT (email) DO NOTHING;

-- Insert Approval Rules
INSERT INTO approval_rules (min_amount, max_amount, approval_levels, required_roles, is_active, priority) VALUES 
(0.00, 10000.00, 1, 'ROLE_MANAGER', TRUE, 1),
(10000.01, 50000.00, 2, 'ROLE_MANAGER,ROLE_FINANCE', TRUE, 2),
(50000.01, 100000.00, 3, 'ROLE_MANAGER,ROLE_FINANCE,ROLE_ADMIN', TRUE, 3),
(100000.01, 999999999.99, 4, 'ROLE_MANAGER,ROLE_FINANCE,ROLE_ADMIN,ROLE_ADMIN', TRUE, 4)
ON CONFLICT DO NOTHING;

-- Insert Invoices
INSERT INTO invoices (vendor_id, amount, cgst_amount, sgst_amount, igst_amount, total_amount, 
                      invoice_date, due_date, status, current_approval_level, is_overdue, invoice_number) VALUES 
((SELECT id FROM vendors WHERE name='ABC Suppliers Pvt Ltd'), 5000.00, 450.00, 450.00, 0.00, 5900.00, 
 '2024-01-15', '2024-02-15', 'APPROVED', 1, FALSE, 'INV-20240115-0001'),
((SELECT id FROM vendors WHERE name='XYZ Trading Company'), 25000.00, 2250.00, 2250.00, 0.00, 29500.00, 
 '2024-01-20', '2024-02-20', 'PENDING', 0, FALSE, 'INV-20240120-0002'),
((SELECT id FROM vendors WHERE name='Tech Solutions Inc'), 75000.00, 6750.00, 6750.00, 0.00, 88500.00, 
 '2024-01-10', '2024-02-10', 'OVERDUE', 2, TRUE, 'INV-20240110-0003'),
((SELECT id FROM vendors WHERE name='Global Imports Ltd'), 15000.00, 1350.00, 1350.00, 0.00, 17700.00, 
 '2024-01-25', '2024-02-25', 'APPROVED', 2, FALSE, 'INV-20240125-0004'),
((SELECT id FROM vendors WHERE name='ABC Suppliers Pvt Ltd'), 120000.00, 10800.00, 10800.00, 0.00, 141600.00, 
 '2024-01-05', '2024-02-05', 'ESCALATED', 3, TRUE, 'INV-20240105-0005')
ON CONFLICT (invoice_number) DO NOTHING;

-- Insert Payments
INSERT INTO payments (invoice_id, amount, payment_date, payment_method, transaction_reference, notes) VALUES 
((SELECT id FROM invoices WHERE invoice_number='INV-20240115-0001'), 5900.00, '2024-02-10 10:30:00', 
 'BANK_TRANSFER', 'TXN-20240210-001', 'Full payment received'),
((SELECT id FROM invoices WHERE invoice_number='INV-20240110-0003'), 30000.00, '2024-02-12 14:20:00', 
 'CHEQUE', 'CHQ-20240212-001', 'Partial payment'),
((SELECT id FROM invoices WHERE invoice_number='INV-20240125-0004'), 17700.00, '2024-02-20 09:15:00', 
 'BANK_TRANSFER', 'TXN-20240220-002', 'Full payment')
ON CONFLICT DO NOTHING;

-- Update invoice status based on payments
UPDATE invoices SET status='PAID' WHERE invoice_number='INV-20240115-0001';
UPDATE invoices SET status='PARTIALLY_PAID' WHERE invoice_number='INV-20240110-0003';
UPDATE invoices SET status='PAID' WHERE invoice_number='INV-20240125-0004';

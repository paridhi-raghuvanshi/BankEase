-- BankEase Database Initialization Script
-- Run this after the application creates the schema to seed initial data

-- Roles
INSERT IGNORE INTO roles (id, name, description) VALUES
(1, 'ROLE_CUSTOMER', 'Standard banking customer'),
(2, 'ROLE_BANKER', 'Bank employee with elevated privileges'),
(3, 'ROLE_ADMIN', 'System administrator with full access');

-- Admin user (password: Admin@12345 - BCrypt encoded)
INSERT IGNORE INTO users (
    customer_id, first_name, last_name, email, phone, password,
    address, pincode, city, state, status, mfa_enabled,
    failed_login_attempts, created_at, updated_at
) VALUES (
    'CUST00000001', 'System', 'Admin', 'admin@bankease.com', '9000000001',
    '$2a$12$D9NQFOiEjIqMVEEEBb.eguOoKdW8IrfLo9cFcHSZJfVQxNWxePLXG',
    '1 MG Road, Bangalore', '560001', 'Bangalore', 'Karnataka',
    'ACTIVE', 0, 0, NOW(), NOW()
);

-- Assign admin role
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@bankease.com' AND r.name = 'ROLE_ADMIN';

-- Demo Customer (password: Customer@123)
INSERT IGNORE INTO users (
    customer_id, first_name, last_name, email, phone, password,
    address, pincode, city, state, status, mfa_enabled,
    failed_login_attempts, created_at, updated_at
) VALUES (
    'CUST12345678', 'Rajesh', 'Kumar', 'rajesh@example.com', '9876543210',
    '$2a$12$LH3L5OoxBF1M7DUe4pC2k.MIkCHxDgjKP7u.0KvCxv1RPb2kFORwa',
    '42 Park Street, Mumbai', '400001', 'Mumbai', 'Maharashtra',
    'ACTIVE', 0, 0, NOW(), NOW()
);

-- Assign customer role
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'rajesh@example.com' AND r.name = 'ROLE_CUSTOMER';

-- Demo savings account for test customer
INSERT IGNORE INTO accounts (
    account_number, ifsc_code, account_type, balance, available_balance,
    currency, status, interest_rate, daily_transfer_limit, monthly_transfer_limit,
    user_id, created_at, updated_at
)
SELECT
    '4001234567890001', 'BKEZ0001001', 'SAVINGS', 125000.00, 125000.00,
    'INR', 'ACTIVE', 3.5000, 100000.00, 1000000.00,
    u.id, NOW(), NOW()
FROM users u WHERE u.email = 'rajesh@example.com';

-- Demo current account for test customer
INSERT IGNORE INTO accounts (
    account_number, ifsc_code, account_type, balance, available_balance,
    currency, status, interest_rate, daily_transfer_limit, monthly_transfer_limit,
    user_id, created_at, updated_at
)
SELECT
    '4001234567890002', 'BKEZ0001001', 'CURRENT', 50000.00, 50000.00,
    'INR', 'ACTIVE', 0.0000, 500000.00, 5000000.00,
    u.id, NOW(), NOW()
FROM users u WHERE u.email = 'rajesh@example.com';

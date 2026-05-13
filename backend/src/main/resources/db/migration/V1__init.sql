CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('OWNER', 'EMPLOYEE')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    email VARCHAR(180) NOT NULL,
    phone VARCHAR(60) NOT NULL,
    address VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    date DATE NOT NULL,
    end_date DATE NOT NULL,
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN ('BRUILOFT', 'VERJAARDAG', 'CONGRES', 'OVERIG')),
    guest_count INTEGER NOT NULL CHECK (guest_count > 0),
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('CONCEPT', 'BEVESTIGD', 'GEANNULEERD')),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    invoice_number VARCHAR(40) NOT NULL UNIQUE,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    vat_amount NUMERIC(12, 2) NOT NULL CHECK (vat_amount >= 0),
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('ONBETAALD', 'BETAALD', 'VERLOPEN')),
    due_date DATE NOT NULL,
    paid_date DATE,
    pdf_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL CHECK (payment_method IN ('BANK', 'CASH', 'PIN')),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE company_settings (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(180) NOT NULL,
    logo_path VARCHAR(500),
    address VARCHAR(255) NOT NULL,
    vat_number VARCHAR(80) NOT NULL,
    iban VARCHAR(80) NOT NULL,
    mail_from VARCHAR(180) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_bookings_date ON bookings(date);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_invoices_status_due_date ON invoices(status, due_date);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);

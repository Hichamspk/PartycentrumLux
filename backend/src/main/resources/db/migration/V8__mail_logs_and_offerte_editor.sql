ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS offerte_customer_message TEXT;

CREATE TABLE IF NOT EXISTS mail_logs (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT,
    bezichtiging_id BIGINT,
    type VARCHAR(60) NOT NULL,
    ontvanger_email VARCHAR(255) NOT NULL,
    onderwerp VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    foutmelding TEXT,
    verzonden_op TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mail_logs_booking_id ON mail_logs(booking_id);
CREATE INDEX IF NOT EXISTS idx_mail_logs_bezichtiging_id ON mail_logs(bezichtiging_id);
CREATE INDEX IF NOT EXISTS idx_mail_logs_type ON mail_logs(type);
CREATE INDEX IF NOT EXISTS idx_mail_logs_status ON mail_logs(status);
CREATE INDEX IF NOT EXISTS idx_mail_logs_verzonden_op ON mail_logs(verzonden_op DESC);

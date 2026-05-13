CREATE TABLE IF NOT EXISTS sub_prijzen (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    naam VARCHAR(180) NOT NULL,
    prijs NUMERIC(12, 2) NOT NULL CHECK (prijs >= 0),
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sub_prijzen_booking_id ON sub_prijzen(booking_id);

INSERT INTO sub_prijzen (booking_id, naam, prijs, position, created_at, updated_at)
SELECT b.id, 'Huur evenementenlocatie', b.price, 0, NOW(), NOW()
FROM bookings b
WHERE b.price > 0
  AND NOT EXISTS (
      SELECT 1
      FROM sub_prijzen sp
      WHERE sp.booking_id = b.id
  );

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS annulerings_reden TEXT;

UPDATE bookings
SET status = 'CONTRACT_ONDERTEKEND'
WHERE status = 'BEVESTIGD';

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS bookings_status_check,
    ADD CONSTRAINT bookings_status_check CHECK (
        status IN (
            'CONCEPT',
            'CONTRACT_VERZONDEN',
            'CONTRACT_ONDERTEKEND',
            'FACTUUR_VERZONDEN',
            'AANBETALING_BETAALD',
            'VOLLEDIG_BETAALD',
            'AFGEROND',
            'GEANNULEERD'
        )
    );

ALTER TABLE invoices
    DROP CONSTRAINT IF EXISTS invoices_booking_id_key,
    ADD COLUMN IF NOT EXISTS invoice_type VARCHAR(30) NOT NULL DEFAULT 'VOLLEDIG',
    ADD COLUMN IF NOT EXISTS invoice_date DATE NOT NULL DEFAULT CURRENT_DATE;

ALTER TABLE invoices
    DROP CONSTRAINT IF EXISTS invoices_invoice_type_check,
    ADD CONSTRAINT invoices_invoice_type_check CHECK (invoice_type IN ('VOLLEDIG', 'AANBETALING', 'RESTANT'));

CREATE INDEX IF NOT EXISTS idx_invoices_booking_id ON invoices(booking_id);
CREATE INDEX IF NOT EXISTS idx_invoices_invoice_type ON invoices(invoice_type);

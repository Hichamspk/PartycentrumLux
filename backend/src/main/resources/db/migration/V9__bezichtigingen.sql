CREATE TABLE IF NOT EXISTS bezichtigingen (
    id BIGSERIAL PRIMARY KEY,
    klant_naam VARCHAR(180) NOT NULL,
    klant_email VARCHAR(180) NOT NULL,
    klant_telefoon VARCHAR(80) NOT NULL,
    datum DATE NOT NULL,
    start_tijd TIME NOT NULL,
    eind_tijd TIME NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'GEPLAND',
    notities TEXT,
    booking_id BIGINT REFERENCES bookings(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT bezichtigingen_status_check CHECK (status IN ('GEPLAND', 'GEWEEST', 'GEANNULEERD')),
    CONSTRAINT bezichtigingen_tijd_check CHECK (eind_tijd > start_tijd)
);

CREATE INDEX IF NOT EXISTS idx_bezichtigingen_datum ON bezichtigingen(datum);
CREATE INDEX IF NOT EXISTS idx_bezichtigingen_status ON bezichtigingen(status);
CREATE INDEX IF NOT EXISTS idx_bezichtigingen_booking_id ON bezichtigingen(booking_id);

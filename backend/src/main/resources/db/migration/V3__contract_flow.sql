ALTER TABLE company_settings
    ADD COLUMN IF NOT EXISTS docuseal_api_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS docuseal_base_url VARCHAR(500) NOT NULL DEFAULT 'http://docuseal:3000',
    ADD COLUMN IF NOT EXISTS general_terms TEXT NOT NULL DEFAULT 'Annulering, schade, geluid en overige afspraken worden conform de algemene voorwaarden van Partycentrum Lux behandeld.';

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS event_date DATE,
    ADD COLUMN IF NOT EXISTS start_time TIME NOT NULL DEFAULT '18:00:00',
    ADD COLUMN IF NOT EXISTS end_time TIME NOT NULL DEFAULT '23:00:00',
    ADD COLUMN IF NOT EXISTS conditions TEXT,
    ADD COLUMN IF NOT EXISTS contract_status VARCHAR(30) NOT NULL DEFAULT 'GEEN',
    ADD COLUMN IF NOT EXISTS docuseal_submission_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS contract_html TEXT,
    ADD COLUMN IF NOT EXISTS contract_signed_date DATE;

UPDATE bookings
SET event_date = COALESCE(event_date, date)
WHERE event_date IS NULL;

ALTER TABLE bookings
    ALTER COLUMN event_date SET NOT NULL,
    ALTER COLUMN date DROP NOT NULL,
    ALTER COLUMN end_date DROP NOT NULL;

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS bookings_contract_status_check,
    ADD CONSTRAINT bookings_contract_status_check CHECK (contract_status IN ('GEEN', 'CONCEPT', 'VERZONDEN', 'ONDERTEKEND'));

CREATE TABLE IF NOT EXISTS booking_properties (
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    property VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bookings_event_date ON bookings(event_date);
CREATE INDEX IF NOT EXISTS idx_bookings_docuseal_submission_id ON bookings(docuseal_submission_id);

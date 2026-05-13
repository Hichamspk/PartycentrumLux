ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS korting NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS aanbetaling_percentage INTEGER NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS offerte_datum DATE,
    ADD COLUMN IF NOT EXISTS ondertekening_datum DATE,
    ADD COLUMN IF NOT EXISTS aanbetaling_betaald BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS aanbetaling_betaald_datum DATE,
    ADD COLUMN IF NOT EXISTS restant_betaald BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS restant_betaald_datum DATE,
    ADD COLUMN IF NOT EXISTS offerte_pdf_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS offerte_sent_date DATE;

UPDATE bookings
SET offerte_datum = COALESCE(offerte_datum, CURRENT_DATE)
WHERE offerte_datum IS NULL;

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS bookings_status_check,
    ADD CONSTRAINT bookings_status_check CHECK (
        status IN (
            'CONCEPT',
            'OFFERTE_VERZONDEN',
            'BEVESTIGD',
            'AANBETALING_BETAALD',
            'VOLLEDIG_BETAALD',
            'AFGEROND',
            'GEANNULEERD',
            'CONTRACT_VERZONDEN',
            'CONTRACT_ONDERTEKEND',
            'FACTUUR_VERZONDEN'
        )
    );

UPDATE bookings
SET status = CASE status
    WHEN 'CONTRACT_VERZONDEN' THEN 'OFFERTE_VERZONDEN'
    WHEN 'CONTRACT_ONDERTEKEND' THEN 'BEVESTIGD'
    WHEN 'FACTUUR_VERZONDEN' THEN 'BEVESTIGD'
    ELSE status
END;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_korting_check CHECK (korting >= 0),
    ADD CONSTRAINT bookings_aanbetaling_percentage_check CHECK (aanbetaling_percentage BETWEEN 0 AND 100);

ALTER TABLE sub_prijzen
    ADD COLUMN IF NOT EXISTS bedrag NUMERIC(12, 2) NOT NULL DEFAULT 0;

UPDATE sub_prijzen
SET bedrag = prijs
WHERE bedrag = 0 AND prijs IS NOT NULL;

ALTER TABLE sub_prijzen
    ADD CONSTRAINT sub_prijzen_bedrag_check CHECK (bedrag >= 0);

ALTER TABLE company_settings
    ADD COLUMN IF NOT EXISTS docuseal_hussain_email VARCHAR(180),
    ADD COLUMN IF NOT EXISTS docuseal_hussain_signature_token TEXT,
    ADD COLUMN IF NOT EXISTS google_review_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS smtp_host VARCHAR(180) NOT NULL DEFAULT 'smtp.gmail.com',
    ADD COLUMN IF NOT EXISTS smtp_port INTEGER NOT NULL DEFAULT 587,
    ADD COLUMN IF NOT EXISTS smtp_username VARCHAR(180),
    ADD COLUMN IF NOT EXISTS smtp_password VARCHAR(500),
    ADD COLUMN IF NOT EXISTS smtp_from VARCHAR(180);

UPDATE company_settings
SET smtp_from = COALESCE(smtp_from, mail_from),
    google_review_url = COALESCE(google_review_url, 'https://www.google.com/search?q=Partycentrum+Lux+review')
WHERE smtp_from IS NULL OR google_review_url IS NULL;

CREATE INDEX IF NOT EXISTS idx_bookings_offerte_datum ON bookings(offerte_datum);
CREATE INDEX IF NOT EXISTS idx_bookings_ondertekening_datum ON bookings(ondertekening_datum);
CREATE INDEX IF NOT EXISTS idx_bookings_betalingen ON bookings(aanbetaling_betaald, restant_betaald);

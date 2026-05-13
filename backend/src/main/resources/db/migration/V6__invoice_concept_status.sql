ALTER TABLE invoices
    DROP CONSTRAINT IF EXISTS invoices_status_check,
    ADD CONSTRAINT invoices_status_check CHECK (status IN ('CONCEPT', 'ONBETAALD', 'BETAALD', 'VERLOPEN'));

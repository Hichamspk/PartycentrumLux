ALTER TABLE company_settings
    ADD COLUMN IF NOT EXISTS docuseal_contract_template_id VARCHAR(80);

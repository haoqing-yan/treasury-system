ALTER TABLE bank_accounts
    ADD COLUMN channel VARCHAR(16) NOT NULL DEFAULT 'BANK' AFTER id;

CREATE INDEX idx_account_channel_status ON bank_accounts (channel, status);

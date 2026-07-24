CREATE TABLE payment_batches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_no VARCHAR(32) NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    status VARCHAR(24) NOT NULL,
    total_amount DECIMAL(20, 2) NOT NULL,
    total_count INT NOT NULL,
    success_count INT NOT NULL,
    failed_count INT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_batch_no UNIQUE (batch_no)
);

CREATE TABLE payment_batch_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    payment_order_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    failure_reason VARCHAR(240) NULL,
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_batch_payment UNIQUE (payment_order_id),
    CONSTRAINT fk_batch_item_batch FOREIGN KEY (batch_id) REFERENCES payment_batches (id),
    CONSTRAINT fk_batch_item_payment FOREIGN KEY (payment_order_id) REFERENCES payment_orders (id)
);

CREATE INDEX idx_payment_batch_schedule ON payment_batches (status, scheduled_at);
CREATE INDEX idx_payment_batch_item_batch ON payment_batch_items (batch_id, status);

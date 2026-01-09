CREATE TABLE payment_orders
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_event_id BIGINT         NOT NULL,
    seller_id        BIGINT         NOT NULL,
    product_id       BIGINT         NOT NULL,
    order_id         VARCHAR(255)   NOT NULL,
    amount           DECIMAL(12, 2) NOT null,
    payment_status   ENUM ('NOT_STARTED', 'EXECUTING', 'SUCCESS', 'FAILURE', 'UNKNOWN') NOT NULL DEFAULT 'NOT_STARTED',
    ledger_updated   BOOLEAN        NOT NULL DEFAULT FALSE,
    waller_updated   BOOLEAN        NOT NULL DEFAULT FALSE,
    fail_count       TINYINT        NOT NULL DEFAULT 0,
    threshold        TINYINT        NOT NULL DEFAULT 5,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (payment_event_id) REFERENCES payment_events (id)
);
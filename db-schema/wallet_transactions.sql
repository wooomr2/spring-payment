CREATE TABLE wallet_transactions
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id       BIGINT              NOT NULL,
    amount          DECIMAL(15, 2)      NOT NULL,
    type            ENUM ('CREDIT', 'DEBIT') NOT NULL,
    reference_id    BIGINT              NOT NULL,
    reference_type  VARCHAR(50)         NOT NULL,
    order_id        VARCHAR(255),
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    created_at      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE outboxes
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    status          ENUM ('INIT', 'FAILURE', 'SUCCESS') DEFAULT 'INIT',
    type            VARCHAR(40),
    partition_key   INT                                 DEFAULT 0,
    payload         JSON,
    metadata        JSON,
    created_at      DATETIME            NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME            NOT NULL        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
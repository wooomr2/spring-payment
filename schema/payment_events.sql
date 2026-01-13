CREATE TABLE payment_events
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id        BIGINT       NOT NULL,
    is_payment_done BOOLEAN      NOT NULL DEFAULT FALSE,
    payment_key     VARCHAR(255) UNIQUE,
    order_id        VARCHAR(255) UNIQUE,
    order_name      VARCHAR(255) NOT NULL,
    type            ENUM ('NORMAL')  NOT NULL,
    method          ENUM ('CARD', 'VIRTUAL', 'EASY_PAY', 'MOBILE', 'TRANSFER', 'CULTURE_GIFT', 'BOOK_CULTURE_GIFT', 'GAME_CULTURE_GIFT') NOT NULL,
    psp_raw_data    JSON,
    approved_at     DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
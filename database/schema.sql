-- Reference DDL (MySQL 8). The running application creates its schema through Hibernate
-- (spring.jpa.hibernate.ddl-auto), which produces an equivalent structure; Hibernate 6 may emit
-- native ENUM columns where this file uses VARCHAR. Use this file to read or review the design.
-- For real production use, adopt Flyway/Liquibase migrations.

CREATE TABLE users (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  name        VARCHAR(100)  NOT NULL,
  email       VARCHAR(150)  NOT NULL,
  password    VARCHAR(100)  NOT NULL,              -- BCrypt hash
  phone       VARCHAR(20)   NOT NULL,
  role        VARCHAR(20)   NOT NULL,              -- USER | ADMIN
  status      VARCHAR(20)   NOT NULL,              -- ACTIVE | INACTIVE | BLOCKED
  created_at  DATETIME(6)   NOT NULL,
  updated_at  DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_users_email UNIQUE (email),
  INDEX idx_users_status (status)
) ENGINE=InnoDB;

CREATE TABLE wallets (
  id          BIGINT         NOT NULL AUTO_INCREMENT,
  user_id     BIGINT         NOT NULL,
  balance     DECIMAL(19,2)  NOT NULL,
  currency    VARCHAR(3)     NOT NULL,
  status      VARCHAR(20)    NOT NULL,             -- ACTIVE | FROZEN
  version     BIGINT         NULL,                 -- optimistic-lock counter
  created_at  DATETIME(6)    NOT NULL,
  updated_at  DATETIME(6)    NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_wallets_user UNIQUE (user_id),     -- exactly one wallet per user
  CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0)   -- database-level "never negative"
) ENGINE=InnoDB;

CREATE TABLE transactions (
  id                     BIGINT         NOT NULL AUTO_INCREMENT,
  transaction_reference  VARCHAR(40)    NOT NULL,
  sender_id              BIGINT         NOT NULL,  -- for ADD_MONEY sender = receiver = the wallet owner
  receiver_id            BIGINT         NOT NULL,
  amount                 DECIMAL(19,2)  NOT NULL,
  currency               VARCHAR(3)     NOT NULL,
  transaction_type       VARCHAR(20)    NOT NULL,  -- TRANSFER | ADD_MONEY
  status                 VARCHAR(20)    NOT NULL,  -- PENDING | PROCESSING | SUCCESS | FAILED | CANCELLED | REVERSED
  risk_level             VARCHAR(20)    NULL,      -- LOW | MEDIUM | HIGH | BLOCKED (NULL = risk checks never ran)
  risk_reasons           VARCHAR(500)   NULL,
  failure_reason         VARCHAR(255)   NULL,
  description            VARCHAR(255)   NULL,
  idempotency_key        VARCHAR(100)   NOT NULL,
  created_at             DATETIME(6)    NOT NULL,
  updated_at             DATETIME(6)    NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_tx_reference UNIQUE (transaction_reference),
  CONSTRAINT uk_tx_sender_idempotency UNIQUE (sender_id, idempotency_key),   -- the idempotency guarantee
  CONSTRAINT fk_tx_sender   FOREIGN KEY (sender_id)   REFERENCES users (id),
  CONSTRAINT fk_tx_receiver FOREIGN KEY (receiver_id) REFERENCES users (id),
  CONSTRAINT chk_tx_amount_positive CHECK (amount > 0),
  INDEX idx_tx_sender_created   (sender_id, created_at),     -- history, velocity and daily-limit queries
  INDEX idx_tx_receiver_created (receiver_id, created_at),
  INDEX idx_tx_status (status),
  INDEX idx_tx_risk_level (risk_level),
  INDEX idx_tx_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE transaction_events (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  transaction_id  BIGINT        NOT NULL,
  event_type      VARCHAR(40)   NOT NULL,
  event_status    VARCHAR(20)   NOT NULL,            -- SUCCESS | FAILURE
  details         VARCHAR(500)  NULL,
  created_at      DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_tx_events_tx FOREIGN KEY (transaction_id) REFERENCES transactions (id),
  INDEX idx_tx_events_tx (transaction_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE notifications (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  user_id         BIGINT        NOT NULL,
  transaction_id  BIGINT        NOT NULL,
  message         VARCHAR(255)  NOT NULL,
  type            VARCHAR(30)   NOT NULL,            -- PAYMENT_SENT | PAYMENT_RECEIVED | PAYMENT_FAILED | WALLET_TOPUP
  status          VARCHAR(20)   NOT NULL,            -- PENDING | SENT | FAILED
  created_at      DATETIME(6)   NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_notifications_tx   FOREIGN KEY (transaction_id) REFERENCES transactions (id),
  CONSTRAINT uk_notification_tx_user_type UNIQUE (transaction_id, user_id, type),  -- makes the Kafka consumer idempotent
  INDEX idx_notifications_user (user_id, created_at)
) ENGINE=InnoDB;

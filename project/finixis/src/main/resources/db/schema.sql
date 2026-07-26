-- Seva Tyres Database Schema
-- PostgreSQL (primary) and H2 compatible
-- balance is computed in the application layer

CREATE TABLE IF NOT EXISTS Customer (
    customer_id   INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_name VARCHAR(200)  NOT NULL,
    location      VARCHAR(300),
    contact       VARCHAR(30),
    email         VARCHAR(200),
    creation_date DATE          NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT uq_customer_email UNIQUE (email)
);

-- Inventory: tyre shop products and services
CREATE TABLE IF NOT EXISTS Inventory (
    item_id            INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_name          VARCHAR(200)  NOT NULL,
    available_quantity INTEGER       NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    unit_price         DECIMAL(15,2) NOT NULL CHECK (unit_price >= 0),
    barcode            VARCHAR(100),
    CONSTRAINT uq_inventory_name UNIQUE (item_name)
);

-- Transaction_Credit: money the customer owes us (payment expected or received)
CREATE TABLE IF NOT EXISTS Transaction_Credit (
    transaction_id   INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id      INTEGER       NOT NULL,
    total_amount     DECIMAL(15,2) NOT NULL CHECK (total_amount >= 0),
    paid_amount      DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    balance          DECIMAL(15,2) NOT NULL DEFAULT 0,
    transaction_date DATE          NOT NULL DEFAULT CURRENT_DATE,
    is_settled       BOOLEAN       NOT NULL DEFAULT FALSE,
    notes            VARCHAR(500),
    CONSTRAINT fk_tc_customer  FOREIGN KEY (customer_id) REFERENCES Customer(customer_id) ON DELETE RESTRICT
);

-- Line items that make up a single Transaction_Credit
CREATE TABLE IF NOT EXISTS Transaction_Credit_Item (
    line_item_id         INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id       INTEGER       NOT NULL,
    item_id              INTEGER       NOT NULL,
    quantity             INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price_snapshot  DECIMAL(15,2) NOT NULL,
    line_total           DECIMAL(15,2) NOT NULL,
    CONSTRAINT fk_tci_transaction FOREIGN KEY (transaction_id) REFERENCES Transaction_Credit(transaction_id) ON DELETE CASCADE,
    CONSTRAINT fk_tci_item        FOREIGN KEY (item_id) REFERENCES Inventory(item_id) ON DELETE RESTRICT
);

-- Transaction_Debit: money the business owes to the customer (returns, refunds)
CREATE TABLE IF NOT EXISTS Transaction_Debit (
    debit_id    INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id INTEGER       NOT NULL,
    amount      DECIMAL(15,2) NOT NULL CHECK (amount > 0),
    debit_date  DATE          NOT NULL DEFAULT CURRENT_DATE,
    notes       VARCHAR(500),
    CONSTRAINT fk_td_customer FOREIGN KEY (customer_id) REFERENCES Customer(customer_id) ON DELETE RESTRICT
);

-- Sale_Transaction: detailed sales record for each tyre shop sale
CREATE TABLE IF NOT EXISTS Sale_Transaction (
    sale_id              INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bill_no              VARCHAR(20),
    sale_date            DATE          NOT NULL DEFAULT CURRENT_DATE,
    particulars          VARCHAR(500)  NOT NULL,
    brand                VARCHAR(200),
    quantity             INTEGER       NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price           DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    inventory_item_id    INTEGER,
    -- Payment breakdown (all amounts in INR, default 0)
    phone_pe             DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (phone_pe >= 0),
    account_transfer     DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (account_transfer >= 0),
    card_swipe           DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (card_swipe >= 0),
    bajaj_finance        DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (bajaj_finance >= 0),
    cash                 DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (cash >= 0),
    cheque               DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (cheque >= 0),
    credit_amount        DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (credit_amount >= 0),
    total                DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    -- Optional customer info (stored denormalized for invoice printing)
    customer_id          INTEGER,
    customer_name        VARCHAR(200),
    customer_email       VARCHAR(200),
    customer_phone       VARCHAR(30),
    customer_address     VARCHAR(500),
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_st_customer FOREIGN KEY (customer_id) REFERENCES Customer(customer_id) ON DELETE SET NULL,
    CONSTRAINT fk_st_inventory FOREIGN KEY (inventory_item_id) REFERENCES Inventory(item_id) ON DELETE SET NULL
);

-- Generated_Report: PDF or Excel files produced from the Transactions or Reports page
CREATE TABLE IF NOT EXISTS Generated_Report (
    export_id     INTEGER       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name     VARCHAR(300)  NOT NULL,
    file_type     VARCHAR(50)   NOT NULL,
    format        VARCHAR(10)   NOT NULL,
    file_path     VARCHAR(1000),
    creation_date TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Alert_Config: configurable SMS/email reminder campaigns
CREATE TABLE IF NOT EXISTS Alert_Config (
    config_id        INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    message_template TEXT         NOT NULL,
    channel          VARCHAR(10)  NOT NULL DEFAULT 'EMAIL',
    interval_days    INTEGER      NOT NULL DEFAULT 7,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    last_run         TIMESTAMP
);

-- Alert_Log: history of sent alerts
CREATE TABLE IF NOT EXISTS Alert_Log (
    log_id       INTEGER    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_id    INTEGER,
    customer_id  INTEGER,
    customer_name VARCHAR(200),
    sent_at      TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    channel      VARCHAR(10),
    status       VARCHAR(20) NOT NULL DEFAULT 'SENT',
    message      TEXT
);

-- Indexes for common JOIN and filter paths
CREATE INDEX IF NOT EXISTS idx_tc_customer  ON Transaction_Credit(customer_id);
CREATE INDEX IF NOT EXISTS idx_tc_date      ON Transaction_Credit(transaction_date);
CREATE INDEX IF NOT EXISTS idx_tc_settled   ON Transaction_Credit(is_settled);
CREATE INDEX IF NOT EXISTS idx_tci_txn      ON Transaction_Credit_Item(transaction_id);
CREATE INDEX IF NOT EXISTS idx_tci_item     ON Transaction_Credit_Item(item_id);
CREATE INDEX IF NOT EXISTS idx_td_customer  ON Transaction_Debit(customer_id);
CREATE INDEX IF NOT EXISTS idx_td_date      ON Transaction_Debit(debit_date);
CREATE INDEX IF NOT EXISTS idx_rpt_date     ON Generated_Report(creation_date);
CREATE INDEX IF NOT EXISTS idx_st_date      ON Sale_Transaction(sale_date);
CREATE INDEX IF NOT EXISTS idx_st_customer  ON Sale_Transaction(customer_id);
CREATE INDEX IF NOT EXISTS idx_st_bill      ON Sale_Transaction(bill_no);

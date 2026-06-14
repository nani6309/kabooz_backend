-- ================================================================
-- KABOOZ GOLI SODA — Sri Rama Krupa Enterprises
-- PostgreSQL Schema — run this ONCE on your Supabase project
-- (Schema tab > SQL Editor > Paste and Run)
-- ================================================================

-- ----------------------------------------------------------------
-- 1. admin_users
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    failed_attempts INT             NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP       NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------
-- 2. customers
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    mobile          VARCHAR(10)     NOT NULL UNIQUE,
    address         TEXT,
    place_of_supply VARCHAR(100)    NOT NULL DEFAULT 'Karnataka',
    gst_number      VARCHAR(15)     NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_customer_mobile ON customers (mobile);

-- ----------------------------------------------------------------
-- 3. orders
-- ----------------------------------------------------------------
-- (Custom enum types order_status and order_source removed for JPA compatibility)

CREATE TABLE IF NOT EXISTS orders (
    id                  BIGSERIAL       PRIMARY KEY,
    invoice_no          VARCHAR(20)     NOT NULL UNIQUE,
    invoice_date        DATE            NOT NULL,
    due_date            DATE,
    customer_id         BIGINT          NOT NULL REFERENCES customers (id),
    taxable_amount      DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    cgst                DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    sgst                DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    grand_total         DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    received_amount     DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    with_gst            BOOLEAN         NOT NULL DEFAULT FALSE,
    gst_number          VARCHAR(15)     NULL,
    customer_shop_name  VARCHAR(160)    NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    notes               TEXT,
    source              VARCHAR(20)     NOT NULL DEFAULT 'HOMEPAGE',
    deleted_at          TIMESTAMP       NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_status   ON orders (status);
CREATE INDEX IF NOT EXISTS idx_order_created  ON orders (created_at);
CREATE INDEX IF NOT EXISTS idx_order_deleted  ON orders (deleted_at);

-- ----------------------------------------------------------------
-- 4. order_items
-- ----------------------------------------------------------------
-- (Custom enum type bottle_type removed for JPA compatibility)

CREATE TABLE IF NOT EXISTS order_items (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    bottle_type         VARCHAR(10)     NOT NULL,
    flavor              VARCHAR(100)    NOT NULL,
    price_per_bottle    INT             NOT NULL,
    quantity            INT             NOT NULL,
    bottles_per_unit    INT             NOT NULL,
    rate_per_unit       DECIMAL(10,2)   NOT NULL,
    tax_per_unit        DECIMAL(10,2)   NOT NULL,
    total_per_unit      DECIMAL(10,2)   NOT NULL,
    taxable_subtotal    DECIMAL(12,2)   NOT NULL,
    tax_subtotal        DECIMAL(12,2)   NOT NULL,
    line_total          DECIMAL(12,2)   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_item_order ON order_items (order_id);

-- ----------------------------------------------------------------
-- 5. invoice_counter  (SELECT ... FOR UPDATE safe incrementing)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoice_counter (
    id          INT     NOT NULL DEFAULT 1 PRIMARY KEY,
    last_value  INT     NOT NULL DEFAULT 182
);

INSERT INTO invoice_counter (id, last_value)
VALUES (1, 182)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- 6. distributors
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS distributors (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(120)    NOT NULL,
    mobile      VARCHAR(15)     NOT NULL,
    shop_name   VARCHAR(160)    NOT NULL,
    address     TEXT            NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'NEW',
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_distributor_status  ON distributors (status);
CREATE INDEX IF NOT EXISTS idx_distributor_mobile  ON distributors (mobile);
CREATE INDEX IF NOT EXISTS idx_distributor_created ON distributors (created_at);

-- ----------------------------------------------------------------
-- 7. Idempotent migrations (safe to re-run)
-- ----------------------------------------------------------------
-- Add gst_number to customers if missing (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'customers' AND column_name = 'gst_number'
    ) THEN
        ALTER TABLE customers ADD COLUMN gst_number VARCHAR(15) NULL;
    END IF;
END $$;

-- Add customer_shop_name to orders if missing (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'customer_shop_name'
    ) THEN
        ALTER TABLE orders ADD COLUMN customer_shop_name VARCHAR(160) NULL;
    END IF;
END $$;

-- ----------------------------------------------------------------
-- 8. Review workflow columns (safe to re-run)
-- ----------------------------------------------------------------
-- Make invoice_no nullable (invoice assigned only after order is accepted)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'invoice_no'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE orders ALTER COLUMN invoice_no DROP NOT NULL;
    END IF;
END $$;

-- Add review_status column if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'review_status'
    ) THEN
        ALTER TABLE orders ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED';
        -- Default existing rows to ACCEPTED so they are treated as finalised invoices
    END IF;
END $$;

-- Add rejection_reason column if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'rejection_reason'
    ) THEN
        ALTER TABLE orders ADD COLUMN rejection_reason TEXT NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_order_review_status ON orders (review_status);


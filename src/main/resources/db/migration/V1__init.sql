-- 상품 기본 정보
CREATE TABLE product
(
    id         BIGSERIAL PRIMARY KEY,
    -- 상품을 구분하는 코드, 상품명과 별개로 관리
    sku        VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 상품별 현재 재고 수량
CREATE TABLE stock
(
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT      NOT NULL UNIQUE REFERENCES product (id),
    -- 재고는 음수가 될 수 없음, 애플리케이션 규칙 안전망
    quantity   INT         NOT NULL CHECK (quantity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 입/출고 기록
CREATE TABLE stock_history
(
    id             BIGSERIAL PRIMARY KEY,
    stock_id       BIGINT      NOT NULL REFERENCES stock (id),
    type           VARCHAR(10) NOT NULL CHECK (type IN ('INBOUND', 'OUTBOUND')),
    -- 변동 수량 1 이상
    quantity       INT         NOT NULL CHECK (quantity > 0),
    -- 변동 직후 잔량
    quantity_after INT         NOT NULL CHECK (quantity_after >= 0),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 상품별 기록을 최신순으로 조회할 때 사용
CREATE INDEX idx_stock_history_stock_id_created_at ON stock_history (stock_id, created_at DESC);

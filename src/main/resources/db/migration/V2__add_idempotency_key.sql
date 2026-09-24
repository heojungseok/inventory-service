-- 중복 요청 방지를 위한 멱등 키 설정
ALTER TABLE stock_history ADD COLUMN idempotency_key VARCHAR(100) UNIQUE;

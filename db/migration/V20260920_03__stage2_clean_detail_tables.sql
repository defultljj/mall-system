CREATE TABLE IF NOT EXISTS tbl_sales_clean_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, batch_no VARCHAR(40) NOT NULL, order_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NULL, product_id VARCHAR(64) NULL, product_name VARCHAR(120) NULL, category VARCHAR(64) NULL,
  order_status VARCHAR(32) NULL, pay_amount DECIMAL(12,2) NULL, quantity INT NULL, order_channel VARCHAR(32) NULL,
  order_time VARCHAR(32) NULL, is_return TINYINT NOT NULL DEFAULT 0, return_amount DECIMAL(12,2) NULL, net_amount DECIMAL(12,2) NULL,
  UNIQUE KEY uk_sales_detail(batch_no,order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tbl_service_clean_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, batch_no VARCHAR(40) NOT NULL, chat_id VARCHAR(80) NOT NULL,
  session_id VARCHAR(80) NOT NULL, user_id VARCHAR(64) NULL, merchant_id VARCHAR(64) NULL,
  message_content TEXT NULL, send_time VARCHAR(32) NULL, intent_type VARCHAR(40) NULL,
  first_response_seconds INT NULL, is_resolved TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_service_detail(batch_no,chat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

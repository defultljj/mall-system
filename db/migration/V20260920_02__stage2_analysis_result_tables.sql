-- 第二阶段分析结果只读落地层；与商城业务表隔离，可按 batch_no 幂等重跑。
CREATE TABLE IF NOT EXISTS tbl_behavior_metric_daily (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, batch_no VARCHAR(40) NOT NULL, stat_date DATE NOT NULL,
  metric_code VARCHAR(64) NOT NULL, metric_name VARCHAR(100) NOT NULL, metric_value DECIMAL(18,2) NOT NULL,
  dimension_type VARCHAR(40) NOT NULL DEFAULT 'ALL', dimension_value VARCHAR(100) NOT NULL DEFAULT '全部', rank_no INT NOT NULL DEFAULT 0,
  calculated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uk_behavior_metric(batch_no,metric_code,dimension_type,dimension_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_sales_metric_daily LIKE tbl_behavior_metric_daily;
CREATE TABLE IF NOT EXISTS tbl_review_metric_daily LIKE tbl_behavior_metric_daily;
CREATE TABLE IF NOT EXISTS tbl_service_metric_daily LIKE tbl_behavior_metric_daily;
CREATE TABLE IF NOT EXISTS tbl_behavior_clean_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, batch_no VARCHAR(40) NOT NULL, log_id VARCHAR(64) NOT NULL, user_id VARCHAR(64) NOT NULL,
  product_id VARCHAR(64) NULL, behavior_type VARCHAR(40) NOT NULL, session_id VARCHAR(80) NULL, behavior_time DATETIME NULL,
  source_file VARCHAR(255) NULL, UNIQUE KEY uk_behavior_detail(batch_no,log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_review_clean_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, batch_no VARCHAR(40) NOT NULL, review_id VARCHAR(64) NOT NULL, order_id VARCHAR(64) NULL,
  user_id VARCHAR(64) NULL, product_id VARCHAR(64) NULL, rating INT NOT NULL, sentiment VARCHAR(16) NOT NULL,
  review_content TEXT NULL, review_time DATETIME NULL, UNIQUE KEY uk_review_detail(batch_no,review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

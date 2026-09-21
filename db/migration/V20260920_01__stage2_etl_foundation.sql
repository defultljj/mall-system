-- 第二阶段ETL运行记录；所有表独立于现有商城业务表，可重复执行。
CREATE TABLE IF NOT EXISTS etl_batch (
  batch_no VARCHAR(40) PRIMARY KEY,
  batch_date DATE NOT NULL,
  status VARCHAR(16) NOT NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  published_at DATETIME NULL,
  source_summary VARCHAR(500) NULL,
  error_message VARCHAR(1000) NULL,
  UNIQUE KEY uk_etl_batch_date (batch_date, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS etl_job_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_no VARCHAR(40) NOT NULL,
  job_name VARCHAR(80) NOT NULL,
  status VARCHAR(16) NOT NULL,
  input_count BIGINT NOT NULL DEFAULT 0,
  output_count BIGINT NOT NULL DEFAULT 0,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME NULL,
  error_message VARCHAR(1000) NULL,
  INDEX idx_etl_job_batch (batch_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS etl_quality_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_no VARCHAR(40) NOT NULL,
  check_name VARCHAR(100) NOT NULL,
  expected_value VARCHAR(100) NULL,
  actual_value VARCHAR(100) NULL,
  passed TINYINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_etl_quality_batch (batch_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS etl_reject_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_no VARCHAR(40) NOT NULL,
  source_type VARCHAR(40) NOT NULL,
  source_file VARCHAR(255) NULL,
  source_row_number BIGINT NULL,
  reject_reason VARCHAR(255) NOT NULL,
  raw_content TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_etl_reject_batch (batch_no, source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

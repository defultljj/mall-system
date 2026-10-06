-- =============================================================================
-- mall_sys_schema.sql
-- 全新安装第一步：mall_sys 全部 38 张表的建表脚本（基础表 + 阶段二分析表）。
--
-- 内容：由开发库 mall_sys 导出的表结构快照（mysqldump --no-data），不含任何数据。
-- 用法（先建库，再执行本脚本，随后按 db/README.md 的顺序导入演示数据）：
--   mysql -uroot -p -e "CREATE DATABASE mall_sys DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
--   mysql -uroot -p --default-character-set=utf8mb4 mall_sys < db/mall_sys_schema.sql
--   mysql -uroot -p --default-character-set=utf8mb4 mall_sys < db/mall_sys_full_demo.sql
--   mysql -uroot -p --default-character-set=utf8mb4 mall_sys < db/mall_sys_expand_demo.sql
--
-- 说明：
--   - 不含 USE / CREATE DATABASE，请先用上面的命令建库（与目录内其他脚本风格一致）。
--   - 全部 CREATE TABLE 均带 IF NOT EXISTS，可安全重复执行；与 db/migration/*.sql
--     中的表结构不冲突（那边同样是 IF NOT EXISTS），全新安装无需再执行迁移脚本。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `etl_batch` (
  `batch_no` varchar(40) NOT NULL,
  `batch_date` date NOT NULL,
  `status` varchar(16) NOT NULL,
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime DEFAULT NULL,
  `published_at` datetime DEFAULT NULL,
  `source_summary` varchar(500) DEFAULT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`batch_no`),
  UNIQUE KEY `uk_etl_batch_date` (`batch_date`,`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `etl_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `job_name` varchar(80) NOT NULL,
  `status` varchar(16) NOT NULL,
  `input_count` bigint NOT NULL DEFAULT '0',
  `output_count` bigint NOT NULL DEFAULT '0',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime DEFAULT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_etl_job_batch` (`batch_no`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `etl_quality_result` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `check_name` varchar(100) NOT NULL,
  `expected_value` varchar(100) DEFAULT NULL,
  `actual_value` varchar(100) DEFAULT NULL,
  `passed` tinyint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_etl_quality_batch` (`batch_no`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `etl_reject_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `source_type` varchar(40) NOT NULL,
  `source_file` varchar(255) DEFAULT NULL,
  `source_row_number` bigint DEFAULT NULL,
  `reject_reason` varchar(255) NOT NULL,
  `raw_content` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_etl_reject_batch` (`batch_no`,`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_name` varchar(80) NOT NULL,
  `activity_type` varchar(20) NOT NULL,
  `discount_value` decimal(12,2) NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `total_count` int NOT NULL DEFAULT '0',
  `claimed_count` int NOT NULL DEFAULT '0',
  `status` varchar(16) NOT NULL DEFAULT '未开始',
  `product_name` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_advertisement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ad_name` varchar(80) NOT NULL,
  `placement` varchar(30) NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `target_url` varchar(255) DEFAULT NULL,
  `weight` int NOT NULL DEFAULT '1',
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT '草稿',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_behavior_clean_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `log_id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `product_id` varchar(64) DEFAULT NULL,
  `behavior_type` varchar(40) NOT NULL,
  `session_id` varchar(80) DEFAULT NULL,
  `behavior_time` datetime DEFAULT NULL,
  `source_file` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_behavior_detail` (`batch_no`,`log_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3435 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_behavior_event` (
  `log_id` bigint NOT NULL,
  `user_id` varchar(32) DEFAULT NULL,
  `product_id` varchar(32) DEFAULT NULL,
  `behavior_type` varchar(20) DEFAULT NULL,
  `source` varchar(30) DEFAULT NULL,
  `search_keyword` varchar(80) DEFAULT NULL,
  `session_id` varchar(40) DEFAULT NULL,
  `device_type` varchar(20) DEFAULT NULL,
  `ip_city` varchar(30) DEFAULT NULL,
  `product_price` decimal(12,2) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `behavior_time` datetime DEFAULT NULL,
  PRIMARY KEY (`log_id`),
  KEY `idx_behavior_time` (`behavior_time`),
  KEY `idx_behavior_type` (`behavior_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_behavior_metric_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `stat_date` date NOT NULL,
  `metric_code` varchar(64) NOT NULL,
  `metric_name` varchar(100) NOT NULL,
  `metric_value` decimal(18,2) NOT NULL,
  `dimension_type` varchar(40) NOT NULL DEFAULT 'ALL',
  `dimension_value` varchar(100) NOT NULL DEFAULT '全部',
  `rank_no` int NOT NULL DEFAULT '0',
  `calculated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_behavior_metric` (`batch_no`,`metric_code`,`dimension_type`,`dimension_value`)
) ENGINE=InnoDB AUTO_INCREMENT=1938 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_brand` (
  `id` int NOT NULL AUTO_INCREMENT,
  `brand_name` varchar(32) DEFAULT NULL,
  `logo_name` varchar(64) DEFAULT NULL,
  `brand_desc` varchar(512) DEFAULT NULL,
  `show_status` int DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1050 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_brand_category` (
  `brand_id` int DEFAULT NULL,
  `brand_name` varchar(32) DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `category_name` varchar(32) DEFAULT NULL,
  UNIQUE KEY `uni_brand_category` (`brand_id`,`category_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_customer_service_session` (
  `session_id` varchar(40) NOT NULL,
  `user_id` varchar(32) DEFAULT NULL,
  `merchant_id` varchar(32) DEFAULT NULL,
  `intent_type` varchar(30) DEFAULT NULL,
  `first_response_seconds` int DEFAULT NULL,
  `is_resolved` tinyint NOT NULL DEFAULT '0',
  `message_count` int NOT NULL DEFAULT '0',
  `started_at` datetime NOT NULL,
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_dept` (
  `id` int NOT NULL AUTO_INCREMENT,
  `dept_name` varchar(64) DEFAULT NULL,
  `dept_desc` varchar(64) DEFAULT NULL,
  `parent_id` int DEFAULT NULL,
  `p_ids` varchar(128) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_goods_attr` (
  `id` int NOT NULL AUTO_INCREMENT,
  `attr_name` varchar(32) DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `attr_type` int DEFAULT NULL,
  `value_type` int DEFAULT NULL,
  `attr_value` varchar(255) DEFAULT NULL,
  `search_enable` int DEFAULT NULL,
  `enable` int DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_attr_category_type` (`category_id`,`attr_type`)
) ENGINE=InnoDB AUTO_INCREMENT=4205 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_goods_attr_group` (
  `id` int NOT NULL AUTO_INCREMENT,
  `group_name` varchar(32) DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `descript` varchar(255) DEFAULT NULL,
  `icons` varchar(255) DEFAULT NULL,
  `sort` int DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_attr_group_category` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=42002 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_goods_attr_value` (
  `id` int NOT NULL AUTO_INCREMENT,
  `spu_id` int DEFAULT NULL,
  `attr_id` int DEFAULT NULL,
  `value_type` int DEFAULT NULL,
  `attr_value` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2074 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_goods_category` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '类别编号',
  `category_name` varchar(32) DEFAULT NULL COMMENT '类别名称',
  `parent_id` int unsigned DEFAULT NULL COMMENT '父类别编号',
  `p_ids` varchar(64) DEFAULT NULL,
  `show_status` tinyint unsigned DEFAULT '1' COMMENT '状态：1上架 2下架',
  `sort` int unsigned DEFAULT NULL COMMENT '优先级',
  `icons` varchar(32) DEFAULT NULL,
  `product_unit` varchar(32) DEFAULT NULL,
  `product_count` int DEFAULT NULL,
  `create_date` datetime DEFAULT NULL COMMENT '创建时间',
  `update_date` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `pid` (`parent_id`) USING BTREE,
  CONSTRAINT `tbl_goods_category_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `tbl_goods_category` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=423 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='商品类别表';

CREATE TABLE IF NOT EXISTS `tbl_group_attr_relation` (
  `id` int NOT NULL AUTO_INCREMENT,
  `attr_group_id` int DEFAULT NULL,
  `attr_id` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1029 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku_code` varchar(32) NOT NULL,
  `product_name` varchar(100) NOT NULL,
  `warehouse` varchar(50) NOT NULL DEFAULT '主仓',
  `stock` int NOT NULL DEFAULT '0',
  `locked_stock` int NOT NULL DEFAULT '0',
  `warn_stock` int NOT NULL DEFAULT '10',
  `cost_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `status` tinyint NOT NULL DEFAULT '1',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sku_code` (`sku_code`)
) ENGINE=InnoDB AUTO_INCREMENT=300213 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_inventory_flow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inventory_id` bigint NOT NULL,
  `flow_no` varchar(48) NOT NULL,
  `change_qty` int NOT NULL,
  `before_qty` int NOT NULL,
  `after_qty` int NOT NULL,
  `biz_type` varchar(20) NOT NULL,
  `remark` varchar(120) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `flow_no` (`flow_no`),
  KEY `idx_inventory_flow_item` (`inventory_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=11334 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(32) NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `phone` varchar(24) DEFAULT NULL,
  `member_level` varchar(20) NOT NULL DEFAULT '普通会员',
  `source` varchar(20) DEFAULT NULL,
  `balance` decimal(12,2) NOT NULL DEFAULT '0.00',
  `points` int NOT NULL DEFAULT '0',
  `total_spent` decimal(12,2) NOT NULL DEFAULT '0.00',
  `status` varchar(16) NOT NULL DEFAULT '正常',
  `registered_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `permission` varchar(32) DEFAULT NULL,
  `path` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_points_flow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `change_points` int NOT NULL,
  `before_points` int NOT NULL,
  `after_points` int NOT NULL,
  `biz_type` varchar(20) NOT NULL,
  `remark` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5584 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_review` (
  `review_id` varchar(32) NOT NULL,
  `order_id` varchar(32) DEFAULT NULL,
  `user_id` varchar(32) DEFAULT NULL,
  `product_id` varchar(32) DEFAULT NULL,
  `rating` int NOT NULL,
  `review_content` varchar(500) DEFAULT NULL,
  `review_time` datetime NOT NULL,
  `is_append` tinyint NOT NULL DEFAULT '0',
  `append_content` varchar(500) DEFAULT NULL,
  `image_count` int DEFAULT '0',
  `praise_num` int DEFAULT '0',
  `merchant_reply` varchar(500) DEFAULT NULL,
  `merchant_reply_time` datetime DEFAULT NULL,
  PRIMARY KEY (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_review_clean_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `review_id` varchar(64) NOT NULL,
  `order_id` varchar(64) DEFAULT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  `product_id` varchar(64) DEFAULT NULL,
  `rating` int NOT NULL,
  `sentiment` varchar(16) NOT NULL,
  `review_content` text,
  `review_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_detail` (`batch_no`,`review_id`)
) ENGINE=InnoDB AUTO_INCREMENT=753 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_review_metric_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `stat_date` date NOT NULL,
  `metric_code` varchar(64) NOT NULL,
  `metric_name` varchar(100) NOT NULL,
  `metric_value` decimal(18,2) NOT NULL,
  `dimension_type` varchar(40) NOT NULL DEFAULT 'ALL',
  `dimension_value` varchar(100) NOT NULL DEFAULT '全部',
  `rank_no` int NOT NULL DEFAULT '0',
  `calculated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_behavior_metric` (`batch_no`,`metric_code`,`dimension_type`,`dimension_value`)
) ENGINE=InnoDB AUTO_INCREMENT=738 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(64) DEFAULT NULL,
  `descript` varchar(64) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_role_permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int DEFAULT NULL,
  `permission_id` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_sales_clean_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `order_id` varchar(64) NOT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  `product_id` varchar(64) DEFAULT NULL,
  `product_name` varchar(120) DEFAULT NULL,
  `category` varchar(64) DEFAULT NULL,
  `order_status` varchar(32) DEFAULT NULL,
  `pay_amount` decimal(12,2) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `order_channel` varchar(32) DEFAULT NULL,
  `order_time` varchar(32) DEFAULT NULL,
  `is_return` tinyint NOT NULL DEFAULT '0',
  `return_amount` decimal(12,2) DEFAULT NULL,
  `net_amount` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sales_detail` (`batch_no`,`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1416 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_sales_metric_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `stat_date` date NOT NULL,
  `metric_code` varchar(64) NOT NULL,
  `metric_name` varchar(100) NOT NULL,
  `metric_value` decimal(18,2) NOT NULL,
  `dimension_type` varchar(40) NOT NULL DEFAULT 'ALL',
  `dimension_value` varchar(100) NOT NULL DEFAULT '全部',
  `rank_no` int NOT NULL DEFAULT '0',
  `calculated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_behavior_metric` (`batch_no`,`metric_code`,`dimension_type`,`dimension_value`)
) ENGINE=InnoDB AUTO_INCREMENT=189 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_sales_order` (
  `order_id` varchar(32) NOT NULL,
  `user_id` varchar(32) NOT NULL,
  `product_id` varchar(32) NOT NULL,
  `product_name` varchar(100) NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `order_status` varchar(16) NOT NULL,
  `pay_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `quantity` int NOT NULL DEFAULT '1',
  `coupon_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `order_channel` varchar(20) DEFAULT NULL,
  `order_time` datetime NOT NULL,
  `pay_time` datetime DEFAULT NULL,
  `delivery_time` datetime DEFAULT NULL,
  `complete_time` datetime DEFAULT NULL,
  `is_return` tinyint NOT NULL DEFAULT '0',
  `return_type` varchar(16) DEFAULT NULL,
  `return_reason` varchar(100) DEFAULT NULL,
  `return_amount` decimal(12,2) DEFAULT NULL,
  `return_time` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  KEY `idx_order_status_time` (`order_status`,`order_time`),
  KEY `idx_order_user` (`user_id`),
  KEY `idx_order_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售订单';

CREATE TABLE IF NOT EXISTS `tbl_service_clean_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `chat_id` varchar(80) NOT NULL,
  `session_id` varchar(80) NOT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  `merchant_id` varchar(64) DEFAULT NULL,
  `message_content` text,
  `send_time` varchar(32) DEFAULT NULL,
  `intent_type` varchar(40) DEFAULT NULL,
  `first_response_seconds` int DEFAULT NULL,
  `is_resolved` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_detail` (`batch_no`,`chat_id`)
) ENGINE=InnoDB AUTO_INCREMENT=402 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_service_metric_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(40) NOT NULL,
  `stat_date` date NOT NULL,
  `metric_code` varchar(64) NOT NULL,
  `metric_name` varchar(100) NOT NULL,
  `metric_value` decimal(18,2) NOT NULL,
  `dimension_type` varchar(40) NOT NULL DEFAULT 'ALL',
  `dimension_value` varchar(100) NOT NULL DEFAULT '全部',
  `rank_no` int NOT NULL DEFAULT '0',
  `calculated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_behavior_metric` (`batch_no`,`metric_code`,`dimension_type`,`dimension_value`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tbl_sku` (
  `id` int NOT NULL AUTO_INCREMENT,
  `spu_id` int DEFAULT NULL COMMENT '所属 SPU id',
  `sku_name` varchar(128) DEFAULT NULL COMMENT '销售属性值组合(分号分隔, 如 256GB;黑铂金)',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `stock` int DEFAULT NULL COMMENT '库存',
  `sku_img` varchar(64) DEFAULT NULL COMMENT 'SKU 图片文件名',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  `sku_code` varchar(32) DEFAULT NULL,
  `sku_status` varchar(16) NOT NULL DEFAULT '启用',
  `discount_price` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=300213 DEFAULT CHARSET=utf8 COMMENT='SKU 表(商品上架第 4 步落库目标)';

CREATE TABLE IF NOT EXISTS `tbl_sku_sale_attr_value` (
  `id` int NOT NULL AUTO_INCREMENT,
  `spu_id` int DEFAULT NULL,
  `sku_id` int DEFAULT NULL,
  `attr_id` int DEFAULT NULL,
  `attr_value` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1423 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_spu_detail` (
  `id` int NOT NULL AUTO_INCREMENT,
  `goods_name` varchar(64) DEFAULT NULL,
  `goods_details` varchar(64) DEFAULT NULL,
  `category_id` int DEFAULT NULL,
  `brand_id` int DEFAULT NULL,
  `weight` float DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  `main_img` varchar(64) DEFAULT NULL COMMENT '商品主图文件名(存 upload/goods 下的文件名)',
  `allow_deduct` int DEFAULT NULL COMMENT '积分设置: 允许抵扣积分',
  `reward_point` int DEFAULT NULL COMMENT '积分设置: 返还积分',
  `lifecycle_status` varchar(16) NOT NULL DEFAULT '已上架',
  PRIMARY KEY (`id`),
  KEY `idx_spu_category_brand` (`category_id`,`brand_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10087 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `account` varchar(32) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `nick_name` varchar(32) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `sex` enum('男','女') DEFAULT NULL,
  `email` varchar(32) DEFAULT NULL,
  `no` varchar(32) DEFAULT NULL,
  `dept_id` int DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1=启用,0=禁用',
  `create_date` datetime DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_dept` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `tbl_user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `role_id` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

-- Course-project completion: inventory, marketing, members, advertisements and analysis samples.
ALTER TABLE tbl_spu_detail ADD COLUMN lifecycle_status VARCHAR(16) NOT NULL DEFAULT '已上架';
ALTER TABLE tbl_sku ADD COLUMN sku_code VARCHAR(32) NULL, ADD COLUMN sku_status VARCHAR(16) NOT NULL DEFAULT '启用', ADD COLUMN discount_price DECIMAL(10,2) NULL;
CREATE TABLE IF NOT EXISTS tbl_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, sku_code VARCHAR(32) NOT NULL UNIQUE,
  product_name VARCHAR(100) NOT NULL, warehouse VARCHAR(50) NOT NULL DEFAULT '主仓',
  stock INT NOT NULL DEFAULT 0, locked_stock INT NOT NULL DEFAULT 0, warn_stock INT NOT NULL DEFAULT 10,
  cost_price DECIMAL(12,2) NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_inventory_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, inventory_id BIGINT NOT NULL, flow_no VARCHAR(48) NOT NULL UNIQUE,
  change_qty INT NOT NULL, before_qty INT NOT NULL, after_qty INT NOT NULL,
  biz_type VARCHAR(20) NOT NULL, remark VARCHAR(120), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_inventory_flow_item (inventory_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_activity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, activity_name VARCHAR(80) NOT NULL, activity_type VARCHAR(20) NOT NULL,
  discount_value DECIMAL(12,2) NOT NULL, start_time DATETIME NOT NULL, end_time DATETIME NOT NULL,
  total_count INT NOT NULL DEFAULT 0, claimed_count INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT '未开始', product_name VARCHAR(100), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id VARCHAR(32) NOT NULL UNIQUE, nickname VARCHAR(50) NOT NULL,
  phone VARCHAR(24), member_level VARCHAR(20) NOT NULL DEFAULT '普通会员', source VARCHAR(20),
  balance DECIMAL(12,2) NOT NULL DEFAULT 0, points INT NOT NULL DEFAULT 0,
  total_spent DECIMAL(12,2) NOT NULL DEFAULT 0, status VARCHAR(16) NOT NULL DEFAULT '正常',
  registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_points_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, member_id BIGINT NOT NULL, change_points INT NOT NULL,
  before_points INT NOT NULL, after_points INT NOT NULL, biz_type VARCHAR(20) NOT NULL,
  remark VARCHAR(100), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_advertisement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, ad_name VARCHAR(80) NOT NULL, placement VARCHAR(30) NOT NULL,
  image_url VARCHAR(255), target_url VARCHAR(255), weight INT NOT NULL DEFAULT 1,
  start_time DATETIME NOT NULL, end_time DATETIME NOT NULL, status VARCHAR(16) NOT NULL DEFAULT '草稿',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_behavior_event (
  log_id BIGINT PRIMARY KEY, user_id VARCHAR(32), product_id VARCHAR(32), behavior_type VARCHAR(20),
  source VARCHAR(30), search_keyword VARCHAR(80), session_id VARCHAR(40), device_type VARCHAR(20),
  ip_city VARCHAR(30), product_price DECIMAL(12,2), quantity INT, behavior_time DATETIME,
  INDEX idx_behavior_time (behavior_time), INDEX idx_behavior_type (behavior_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_review (
  review_id VARCHAR(32) PRIMARY KEY, order_id VARCHAR(32), user_id VARCHAR(32), product_id VARCHAR(32),
  rating INT NOT NULL, review_content VARCHAR(500), review_time DATETIME NOT NULL,
  is_append TINYINT NOT NULL DEFAULT 0, append_content VARCHAR(500), image_count INT DEFAULT 0,
  praise_num INT DEFAULT 0, merchant_reply VARCHAR(500), merchant_reply_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS tbl_customer_service_session (
  session_id VARCHAR(40) PRIMARY KEY, user_id VARCHAR(32), merchant_id VARCHAR(32), intent_type VARCHAR(30),
  first_response_seconds INT, is_resolved TINYINT NOT NULL DEFAULT 0, message_count INT NOT NULL DEFAULT 0,
  started_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO tbl_inventory(id,sku_code,product_name,warehouse,stock,locked_stock,warn_stock,cost_price,status) VALUES
(1,'SKU-P10005','保温杯','广州仓',30,2,10,35.00,1),(2,'SKU-P10002','智能手机','广州仓',8,1,5,980.00,1),
(3,'SKU-P10010','跑步鞋','上海仓',18,0,8,210.00,1),(4,'SKU-P10008','洗发水','深圳仓',6,0,10,28.00,1),
(5,'SKU-P10006','儿童绘本','广州仓',20,3,8,18.00,1),(6,'SKU-P10004','洗衣液','北京仓',42,0,12,43.00,1);
INSERT IGNORE INTO tbl_activity(id,activity_name,activity_type,discount_value,start_time,end_time,total_count,claimed_count,status,product_name) VALUES
(1,'国庆满减','满减',50,'2026-09-20 00:00:00','2026-10-08 23:59:59',500,126,'未开始','全场商品'),
(2,'数码九折','折扣',90,'2026-09-18 00:00:00','2026-09-30 23:59:59',200,58,'进行中','智能手机'),
(3,'绘本特惠','特惠',59,'2026-10-01 00:00:00','2026-10-15 23:59:59',100,0,'未开始','儿童绘本'),
(4,'开学季优惠','满减',20,'2026-08-20 00:00:00','2026-09-10 23:59:59',300,300,'已结束','学习用品');
INSERT IGNORE INTO tbl_member(id,user_id,nickname,phone,member_level,source,balance,points,total_spent,status,registered_at) VALUES
(1,'U10001','林晓','138****1001','黄金会员','APP',234.00,1680,6899.00,'正常','2026-01-12 09:10:00'),
(2,'U10002','陈航','138****1002','VIP会员','小程序',80.50,920,3299.00,'正常','2026-03-18 14:22:00'),
(3,'U10003','周宁','138****1003','普通会员','PC网页',0.00,240,698.00,'正常','2026-05-09 11:45:00'),
(4,'U10004','沈悦','138****1004','普通会员','APP',12.00,60,159.80,'黑名单','2026-06-21 16:30:00'),
(5,'U10005','顾安','138****1005','黄金会员','APP',310.00,2140,8560.00,'正常','2025-12-03 08:15:00');
INSERT IGNORE INTO tbl_advertisement(id,ad_name,placement,image_url,target_url,weight,start_time,end_time,status) VALUES
(1,'国庆主会场','首页轮播','', '/activity/1',100,'2026-09-20 00:00:00','2026-10-08 23:59:59','已上架'),
(2,'数码好物推荐','首页横幅','', '/category/digital',80,'2026-09-18 00:00:00','2026-09-30 23:59:59','已上架'),
(3,'绘本专题','分类页焦点图','', '/product/P10006',60,'2026-10-01 00:00:00','2026-10-15 23:59:59','草稿');
INSERT IGNORE INTO tbl_behavior_event VALUES
(100001,'U10001','P10001','浏览','首页推荐','', 'SESS20260911001','Android','北京',299,0,'2026-09-11 10:12:00'),
(100002,'U10001','P10003','搜索','站内搜索','无线蓝牙耳机','SESS20260911001','Android','北京',199,0,'2026-09-11 10:13:00'),
(100003,'U10001','P10003','收藏','搜索页','无线蓝牙耳机','SESS20260911001','Android','北京',199,0,'2026-09-11 10:13:00'),
(100004,'U10001','P10005','加入购物车','详情页','', 'SESS20260911001','Android','北京',89.9,2,'2026-09-11 10:16:00'),
(100005,'U10001','P10005','购买','购物车','', 'SESS20260911001','Android','北京',89.9,2,'2026-09-11 10:16:00'),
(100006,'U10002','P10002','浏览','直播带货','', 'SESS20260911002','iOS','广州',1299,0,'2026-09-11 11:02:00'),
(100007,'U10002','P10002','加入购物车','直播带货','', 'SESS20260911002','iOS','广州',1299,1,'2026-09-11 11:03:00'),
(100008,'U10003','P10010','搜索','广告落地页','运动鞋','SESS20260911003','Web','上海',349,0,'2026-09-11 12:40:00'),
(100009,'U10003','P10010','浏览','广告落地页','运动鞋','SESS20260911003','Web','上海',349,0,'2026-09-11 12:41:00'),
(100010,'U10004','P10008','收藏','商品详情分享','', 'SESS20260911004','iOS','深圳',59.9,0,'2026-09-11 14:22:00');
INSERT IGNORE INTO tbl_review VALUES
('R20260915001','0202609110001','U10001','P10005',5,'保温效果很好，外观也漂亮，物流很快！','2026-09-15 16:20:00',0,'',1,12,'感谢您的认可，欢迎下次光临~','2026-09-15 17:00:00'),
('R20260916001','0202609110003','U10003','P10010',2,'鞋子偏小一码，穿了半天脚疼，已经退货了。','2026-09-16 20:30:00',1,'客服处理态度还行，退货退款很快。',1,3,'非常抱歉给您带来不好的体验。','2026-09-16 21:05:00'),
('R20260915002','0202609110005','U10005','P10006',4,'绘本内容不错，就是有一本边角磕坏了。','2026-09-15 19:40:00',0,'',0,8,'破损问题已经安排补发。','2026-09-15 20:10:00'),
('R20260911003','0202609110002','U10002','P10002',5,'手机性价比超高，续航一天没问题。','2026-09-13 10:15:00',0,'',3,25,NULL,NULL);
INSERT IGNORE INTO tbl_customer_service_session VALUES
('SESS20260911001','U10001','M1001','售前咨询',5,1,3,'2026-09-11 09:10:00'),
('SESS20260911002','U10003','M1005','物流咨询',120,0,2,'2026-09-16 12:30:00'),
('SESS20260911003','U10004','M1008','投诉维权',90,0,2,'2026-09-17 08:05:00'),
('SESS20260911004','U10002','M1002','售前咨询',2,1,2,'2026-09-11 11:20:00');

UPDATE tbl_sku SET sku_code=CONCAT('SKU-',LPAD(id,6,'0')) WHERE sku_code IS NULL;

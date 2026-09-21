-- Order management foundation and the six sample orders supplied with the requirements.
CREATE TABLE IF NOT EXISTS tbl_sales_order (
    order_id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL,
    product_id VARCHAR(32) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    order_status VARCHAR(16) NOT NULL,
    pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    quantity INT NOT NULL DEFAULT 1,
    coupon_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    order_channel VARCHAR(20),
    order_time DATETIME NOT NULL,
    pay_time DATETIME NULL,
    delivery_time DATETIME NULL,
    complete_time DATETIME NULL,
    is_return TINYINT NOT NULL DEFAULT 0,
    return_type VARCHAR(16) NULL,
    return_reason VARCHAR(100) NULL,
    return_amount DECIMAL(12,2) NULL,
    return_time DATETIME NULL,
    remark VARCHAR(255) NULL,
    INDEX idx_order_status_time (order_status, order_time),
    INDEX idx_order_user (user_id),
    INDEX idx_order_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单';

INSERT IGNORE INTO tbl_sales_order
(order_id,user_id,product_id,product_name,category,order_status,pay_amount,quantity,coupon_amount,
 order_channel,order_time,pay_time,delivery_time,complete_time,is_return,return_type,return_reason,return_amount,return_time,remark)
VALUES
('0202609110001','U10001','P10005','保温杯','家居日用','已完成',179.80,2,10.00,'APP','2026-09-11 10:16:55','2026-09-11 10:17:20','2026-09-12 09:00:00','2026-09-14 15:30:00',0,NULL,NULL,NULL,NULL,NULL),
('0202609110002','U10002','P10002','智能手机','数码3C','待发货',1299.00,1,0.00,'小程序','2026-09-11 11:03:30','2026-09-11 11:04:01',NULL,NULL,0,NULL,NULL,NULL,NULL,NULL),
('0202609110003','U10003','P10010','跑步鞋','服饰鞋包','已完成',349.00,1,20.00,'PC网页','2026-09-11 12:41:05','2026-09-11 12:41:50','2026-09-13 10:00:00','2026-09-15 18:20:00',1,'退货','尺码不合适',349.00,'2026-09-16 09:12:00',NULL),
('0202609110004','U10004','P10008','洗发水','个护美妆','已取消',59.90,1,0.00,'APP','2026-09-11 14:23:00','2026-09-11 14:25:10',NULL,NULL,0,NULL,NULL,NULL,NULL,'付款后取消'),
('0202609110005','U10005','P10006','儿童绘本','图书文娱','已完成',78.00,3,5.00,'APP','2026-09-11 15:02:33','2026-09-11 15:03:12','2026-09-12 11:30:00','2026-09-14 20:10:00',1,'换货','商品破损',0.00,'2026-09-15 10:00:00',NULL),
('0202609110006','U10006','P10004','洗衣液','家居日用','待付款',109.00,2,0.00,'APP','2026-09-11 16:40:00',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL);

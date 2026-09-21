-- mall_sys coherent demonstration dataset
-- Generated for the local course project. All people and contact details are fictional.
-- Re-runnable against the current mall_sys schema.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE tbl_sku_sale_attr_value;
TRUNCATE TABLE tbl_goods_attr_value;
TRUNCATE TABLE tbl_group_attr_relation;
TRUNCATE TABLE tbl_sku;
TRUNCATE TABLE tbl_spu_detail;
TRUNCATE TABLE tbl_goods_attr;
TRUNCATE TABLE tbl_goods_attr_group;
TRUNCATE TABLE tbl_brand_category;
TRUNCATE TABLE tbl_brand;
TRUNCATE TABLE tbl_goods_category;
TRUNCATE TABLE tbl_inventory_flow;
TRUNCATE TABLE tbl_inventory;
TRUNCATE TABLE tbl_points_flow;
TRUNCATE TABLE tbl_review;
TRUNCATE TABLE tbl_customer_service_session;
TRUNCATE TABLE tbl_behavior_event;
TRUNCATE TABLE tbl_sales_order;
TRUNCATE TABLE tbl_activity;
TRUNCATE TABLE tbl_advertisement;
TRUNCATE TABLE tbl_member;
TRUNCATE TABLE tbl_role_permission;
TRUNCATE TABLE tbl_user_role;
TRUNCATE TABLE tbl_permission;
TRUNCATE TABLE tbl_user;
TRUNCATE TABLE tbl_role;
TRUNCATE TABLE tbl_dept;

-- Organization and access control. All demo users use the documented password: 123456.
INSERT INTO tbl_dept(id,dept_name,dept_desc,parent_id,p_ids,create_date,update_date) VALUES
(1,'商城总部','商城经营与管理中心',0,'0','2026-01-05 09:00:00','2026-09-01 09:00:00'),
(10,'商品运营中心','商品、类目和品牌运营',1,'0,1','2026-01-06 09:00:00','2026-09-01 09:00:00'),
(20,'交易履约中心','订单、仓储和售后履约',1,'0,1','2026-01-06 09:10:00','2026-09-01 09:00:00'),
(30,'营销增长中心','活动、广告和用户增长',1,'0,1','2026-01-06 09:20:00','2026-09-01 09:00:00'),
(40,'客户体验中心','会员与客服运营',1,'0,1','2026-01-06 09:30:00','2026-09-01 09:00:00'),
(50,'数据技术中心','数据分析和系统维护',1,'0,1','2026-01-06 09:40:00','2026-09-01 09:00:00'),
(60,'财务风控中心','财务审核和风险控制',1,'0,1','2026-01-06 09:50:00','2026-09-01 09:00:00');

INSERT INTO tbl_role(id,role_name,descript,create_date,update_date) VALUES
(1,'系统管理员','拥有全部管理权限','2026-01-05 09:00:00','2026-09-01 09:00:00'),
(2,'商品运营','管理类目、品牌、属性和商品','2026-01-05 09:10:00','2026-09-01 09:00:00'),
(3,'仓储履约','管理订单、库存和售后','2026-01-05 09:20:00','2026-09-01 09:00:00'),
(4,'营销运营','管理会员、活动和广告','2026-01-05 09:30:00','2026-09-01 09:00:00'),
(5,'客服专员','查看订单、会员和客服数据','2026-01-05 09:40:00','2026-09-01 09:00:00'),
(6,'数据分析师','查看工作台和分析中心','2026-01-05 09:50:00','2026-09-01 09:00:00');

INSERT INTO tbl_permission(id,permission,path) VALUES
(1,'经营概览','/dashboard'),(2,'商品中心','/goods'),(3,'交易中心','/order'),
(4,'客户中心','/member'),(5,'营销中心','/marketing'),(6,'数据中心','/analysis'),(7,'系统设置','/system');

INSERT INTO tbl_role_permission(id,role_id,permission_id) VALUES
(1,1,1),(2,1,2),(3,1,3),(4,1,4),(5,1,5),(6,1,6),(7,1,7),
(8,2,1),(9,2,2),(10,3,1),(11,3,3),(12,4,1),(13,4,4),(14,4,5),
(15,5,1),(16,5,3),(17,5,4),(18,6,1),(19,6,6);

INSERT INTO tbl_user(id,account,password,nick_name,phone,sex,email,no,dept_id,status,create_date,update_date) VALUES
(1,'admin','$2a$10$YkSVdnV4JLEnCtSJaUK0D.U9FPOKTYzTrp5vrz9YBNCbTgcpoDMse','系统管理员','13800001001','男','admin@demo-mall.cn','EMP-0001',1,1,'2026-01-05 09:00:00','2026-09-18 18:00:00'),
(2,'product.li','$2a$10$YkSVdnV4JLEnCtSJaUK0D.U9FPOKTYzTrp5vrz9YBNCbTgcpoDMse','李静','13800001002','女','lij@demo-mall.cn','EMP-0002',10,1,'2026-02-10 09:15:00','2026-09-18 17:20:00'),
(3,'warehouse.zhou','$2a$10$YkSVdnV4JLEnCtSJaUK0D.U9FPOKTYzTrp5vrz9YBNCbTgcpoDMse','周凯','13800001003','男','zhouk@demo-mall.cn','EMP-0003',20,1,'2026-02-12 10:20:00','2026-09-18 16:40:00'),
(4,'order.wang','$2a$10$YkSVdnV4JLEnCtSJaUK0D.U9FPOKTYzTrp5vrz9YBNCbTgcpoDMse','王敏','13800001004','女','wangm@demo-mall.cn','EMP-0004',20,1,'2026-03-03 11:00:00','2026-09-18 16:30:00'),
(5,'marketing.chen','$2a$10$YkSVdnV4JLEnCtSJaUK0D.U9FPOKTYzTrp5vrz9YBNCbTgcpoDMse','陈晨','13800001005','女','chenc@demo-mall.cn','EMP-0005',30,1,'2026-03-18 09:40:00','2026-09-18 15:50:00'),
(6,'service.sun','$2a$10$YkSVdnV4JLEnCtSJaUK0D.U9FPOKTYzTrp5vrz9YBNCbTgcpoDMse','孙浩','13800001006','男','sunh@demo-mall.cn','EMP-0006',40,1,'2026-04-08 13:30:00','2026-09-18 18:10:00'),
(7,'analyst.liu','$2a$10$YkSVdnV4JLEnCtSJaUK0D.U9FPOKTYzTrp5vrz9YBNCbTgcpoDMse','刘悦','13800001007','女','liuy@demo-mall.cn','EMP-0007',50,1,'2026-05-15 10:10:00','2026-09-18 18:15:00'),
(8,'finance.zhao','$2a$10$YkSVdnV4JLEnCtSJaUK0D.U9FPOKTYzTrp5vrz9YBNCbTgcpoDMse','赵磊','13800001008','男','zhaol@demo-mall.cn','EMP-0008',60,1,'2026-06-01 09:30:00','2026-09-18 17:45:00');

INSERT INTO tbl_user_role(id,user_id,role_id) VALUES
(1,1,1),(2,2,2),(3,3,3),(4,4,3),(5,5,4),(6,6,5),(7,7,6),(8,8,6);

-- Category tree.
INSERT INTO tbl_goods_category(id,category_name,parent_id,p_ids,show_status,sort,icons,product_unit,product_count,create_date,update_date) VALUES
(100,'数码家电',0,'0',1,1,NULL,'件',4,'2026-02-01 09:00:00','2026-09-01 09:00:00'),
(110,'手机通讯',100,'0,100',1,1,NULL,'部',2,'2026-02-01 09:05:00','2026-09-01 09:00:00'),
(111,'5G手机',110,'0,100,110',1,1,NULL,'部',2,'2026-02-01 09:10:00','2026-09-01 09:00:00'),
(120,'电脑办公',100,'0,100',1,2,NULL,'台',2,'2026-02-01 09:15:00','2026-09-01 09:00:00'),
(121,'轻薄笔记本',120,'0,100,120',1,1,NULL,'台',2,'2026-02-01 09:20:00','2026-09-01 09:00:00'),
(200,'家居生活',0,'0',1,2,NULL,'件',4,'2026-02-01 09:25:00','2026-09-01 09:00:00'),
(210,'厨房电器',200,'0,200',1,1,NULL,'台',2,'2026-02-01 09:30:00','2026-09-01 09:00:00'),
(211,'电饭煲',210,'0,200,210',1,1,NULL,'台',2,'2026-02-01 09:35:00','2026-09-01 09:00:00'),
(220,'生活日用',200,'0,200',1,2,NULL,'件',2,'2026-02-01 09:40:00','2026-09-01 09:00:00'),
(221,'保温杯',220,'0,200,220',1,1,NULL,'个',2,'2026-02-01 09:45:00','2026-09-01 09:00:00'),
(300,'服饰运动',0,'0',1,3,NULL,'件',2,'2026-02-01 09:50:00','2026-09-01 09:00:00'),
(310,'运动鞋服',300,'0,300',1,1,NULL,'双',2,'2026-02-01 09:55:00','2026-09-01 09:00:00'),
(311,'跑步鞋',310,'0,300,310',1,1,NULL,'双',2,'2026-02-01 10:00:00','2026-09-01 09:00:00'),
(400,'美妆个护',0,'0',1,4,NULL,'件',2,'2026-02-01 10:05:00','2026-09-01 09:00:00'),
(410,'洗护用品',400,'0,400',1,1,NULL,'瓶',1,'2026-02-01 10:10:00','2026-09-01 09:00:00'),
(411,'洗发护发',410,'0,400,410',1,1,NULL,'瓶',1,'2026-02-01 10:15:00','2026-09-01 09:00:00'),
(420,'护肤用品',400,'0,400',1,2,NULL,'瓶',1,'2026-02-01 10:20:00','2026-09-01 09:00:00'),
(421,'面部精华',420,'0,400,420',1,1,NULL,'瓶',1,'2026-02-01 10:25:00','2026-09-01 09:00:00');

INSERT INTO tbl_brand(id,brand_name,logo_name,brand_desc,show_status,create_date,update_date) VALUES
(1001,'Apple','d01.png','消费电子品牌',1,'2026-02-02 09:00:00','2026-09-01 09:00:00'),
(1002,'华为','d02.png','智能终端与通信品牌',1,'2026-02-02 09:05:00','2026-09-01 09:00:00'),
(1003,'联想','d03.png','电脑与智能设备品牌',1,'2026-02-02 09:10:00','2026-09-01 09:00:00'),
(1004,'美的','d04.png','综合家电品牌',1,'2026-02-02 09:15:00','2026-09-01 09:00:00'),
(1005,'苏泊尔','d05.png','厨房电器品牌',1,'2026-02-02 09:20:00','2026-09-01 09:00:00'),
(1006,'膳魔师','d06.png','保温器皿品牌',1,'2026-02-02 09:25:00','2026-09-01 09:00:00'),
(1007,'乐扣乐扣','d07.png','生活用品品牌',1,'2026-02-02 09:30:00','2026-09-01 09:00:00'),
(1008,'安踏','d08.png','运动鞋服品牌',1,'2026-02-02 09:35:00','2026-09-01 09:00:00'),
(1009,'李宁','d09.png','运动用品品牌',1,'2026-02-02 09:40:00','2026-09-01 09:00:00'),
(1010,'欧莱雅','d10.png','专业洗护品牌',1,'2026-02-02 09:45:00','2026-09-01 09:00:00'),
(1011,'珀莱雅','463d149a26674a8b.png','专业护肤品牌',1,'2026-02-02 09:50:00','2026-09-01 09:00:00');

INSERT INTO tbl_brand_category(brand_id,brand_name,category_id,category_name) VALUES
(1001,'Apple',111,'5G手机'),(1002,'华为',111,'5G手机'),(1002,'华为',121,'轻薄笔记本'),
(1003,'联想',121,'轻薄笔记本'),(1004,'美的',211,'电饭煲'),(1005,'苏泊尔',211,'电饭煲'),
(1006,'膳魔师',221,'保温杯'),(1007,'乐扣乐扣',221,'保温杯'),(1008,'安踏',311,'跑步鞋'),
(1009,'李宁',311,'跑步鞋'),(1010,'欧莱雅',411,'洗发护发'),(1011,'珀莱雅',421,'面部精华');

-- Attribute definitions: each active leaf category has specifications and sales attributes.
INSERT INTO tbl_goods_attr_group(id,group_name,category_id,descript,icons,sort,create_date,update_date) VALUES
(11001,'手机核心参数',111,'手机硬件与网络参数',NULL,1,'2026-02-03 09:00:00','2026-09-01 09:00:00'),
(12001,'电脑核心参数',121,'电脑处理器与内存参数',NULL,1,'2026-02-03 09:05:00','2026-09-01 09:00:00'),
(21001,'厨电核心参数',211,'容量与加热参数',NULL,1,'2026-02-03 09:10:00','2026-09-01 09:00:00'),
(22001,'杯具核心参数',221,'材质与保温参数',NULL,1,'2026-02-03 09:15:00','2026-09-01 09:00:00'),
(31001,'鞋类核心参数',311,'鞋面与中底参数',NULL,1,'2026-02-03 09:20:00','2026-09-01 09:00:00'),
(41001,'洗护核心参数',411,'功效与适用发质',NULL,1,'2026-02-03 09:25:00','2026-09-01 09:00:00'),
(42001,'护肤核心参数',421,'功效与适用肤质',NULL,1,'2026-02-03 09:30:00','2026-09-01 09:00:00');

INSERT INTO tbl_goods_attr(id,attr_name,category_id,attr_type,value_type,attr_value,search_enable,enable,create_date,update_date) VALUES
(1101,'屏幕尺寸',111,1,1,NULL,1,1,'2026-02-03 10:00:00','2026-09-01 09:00:00'),(1102,'网络制式',111,1,1,NULL,1,1,'2026-02-03 10:01:00','2026-09-01 09:00:00'),(1103,'存储容量',111,2,2,NULL,1,1,'2026-02-03 10:02:00','2026-09-01 09:00:00'),(1104,'颜色',111,2,2,NULL,1,1,'2026-02-03 10:03:00','2026-09-01 09:00:00'),
(1201,'处理器',121,1,1,NULL,1,1,'2026-02-03 10:04:00','2026-09-01 09:00:00'),(1202,'运行内存',121,1,1,NULL,1,1,'2026-02-03 10:05:00','2026-09-01 09:00:00'),(1203,'硬盘容量',121,2,2,NULL,1,1,'2026-02-03 10:06:00','2026-09-01 09:00:00'),(1204,'机身颜色',121,2,2,NULL,1,1,'2026-02-03 10:07:00','2026-09-01 09:00:00'),
(2101,'加热方式',211,1,1,NULL,1,1,'2026-02-03 10:08:00','2026-09-01 09:00:00'),(2102,'额定功率',211,1,1,NULL,1,1,'2026-02-03 10:09:00','2026-09-01 09:00:00'),(2103,'容量',211,2,2,NULL,1,1,'2026-02-03 10:10:00','2026-09-01 09:00:00'),(2104,'颜色',211,2,2,NULL,1,1,'2026-02-03 10:11:00','2026-09-01 09:00:00'),
(2201,'内胆材质',221,1,1,NULL,1,1,'2026-02-03 10:12:00','2026-09-01 09:00:00'),(2202,'保温时长',221,1,1,NULL,1,1,'2026-02-03 10:13:00','2026-09-01 09:00:00'),(2203,'容量',221,2,2,NULL,1,1,'2026-02-03 10:14:00','2026-09-01 09:00:00'),(2204,'颜色',221,2,2,NULL,1,1,'2026-02-03 10:15:00','2026-09-01 09:00:00'),
(3101,'鞋面材质',311,1,1,NULL,1,1,'2026-02-03 10:16:00','2026-09-01 09:00:00'),(3102,'中底科技',311,1,1,NULL,1,1,'2026-02-03 10:17:00','2026-09-01 09:00:00'),(3103,'尺码',311,2,2,NULL,1,1,'2026-02-03 10:18:00','2026-09-01 09:00:00'),(3104,'颜色',311,2,2,NULL,1,1,'2026-02-03 10:19:00','2026-09-01 09:00:00'),
(4101,'主要功效',411,1,1,NULL,1,1,'2026-02-03 10:20:00','2026-09-01 09:00:00'),(4102,'适用发质',411,1,1,NULL,1,1,'2026-02-03 10:21:00','2026-09-01 09:00:00'),(4103,'净含量',411,2,2,NULL,1,1,'2026-02-03 10:22:00','2026-09-01 09:00:00'),(4104,'香型',411,2,2,NULL,1,1,'2026-02-03 10:23:00','2026-09-01 09:00:00'),
(4201,'主要功效',421,1,1,NULL,1,1,'2026-02-03 10:24:00','2026-09-01 09:00:00'),(4202,'适用肤质',421,1,1,NULL,1,1,'2026-02-03 10:25:00','2026-09-01 09:00:00'),(4203,'规格',421,2,2,NULL,1,1,'2026-02-03 10:26:00','2026-09-01 09:00:00'),(4204,'赠品',421,2,2,NULL,0,1,'2026-02-03 10:27:00','2026-09-01 09:00:00');

INSERT INTO tbl_group_attr_relation(id,attr_group_id,attr_id) VALUES
(1,11001,1101),(2,11001,1102),(3,12001,1201),(4,12001,1202),(5,21001,2101),(6,21001,2102),
(7,22001,2201),(8,22001,2202),(9,31001,3101),(10,31001,3102),(11,41001,4101),(12,41001,4102),
(13,42001,4201),(14,42001,4202);

-- Selectable attribute definitions.
INSERT INTO tbl_goods_attr_value(id,spu_id,attr_id,value_type,attr_value) VALUES
(1,NULL,1103,2,'128GB'),(2,NULL,1103,2,'256GB'),(3,NULL,1103,2,'512GB'),(4,NULL,1104,2,'黑色'),(5,NULL,1104,2,'白色'),
(6,NULL,1203,2,'512GB'),(7,NULL,1203,2,'1TB'),(8,NULL,1204,2,'银色'),(9,NULL,1204,2,'灰色'),
(10,NULL,2103,2,'4L'),(11,NULL,2103,2,'5L'),(12,NULL,2104,2,'白色'),(13,NULL,2104,2,'灰色'),
(14,NULL,2203,2,'500ml'),(15,NULL,2203,2,'600ml'),(16,NULL,2204,2,'黑色'),(17,NULL,2204,2,'蓝色'),
(18,NULL,3103,2,'42'),(19,NULL,3103,2,'43'),(20,NULL,3104,2,'黑色'),(21,NULL,3104,2,'白色'),
(22,NULL,4103,2,'500ml'),(23,NULL,4103,2,'700ml'),(24,NULL,4104,2,'清新花香'),(25,NULL,4104,2,'淡雅木香'),
(26,NULL,4203,2,'30ml'),(27,NULL,4203,2,'50ml'),(28,NULL,4204,2,'无赠品'),(29,NULL,4204,2,'旅行装');

-- SPUs and their selected specification values.
INSERT INTO tbl_spu_detail(id,goods_name,goods_details,category_id,brand_id,weight,create_date,update_date,main_img,allow_deduct,reward_point,lifecycle_status) VALUES
(10001,'Apple iPhone 16','A18芯片，支持5G网络',111,1001,0.17,'2026-03-01 10:00:00','2026-09-15 09:00:00','product-iphone16.jpg',500,600,'已上架'),
(10002,'华为 Mate 70','旗舰影像与卫星通信',111,1002,0.21,'2026-03-02 10:00:00','2026-09-15 09:00:00','product-mate70.png',500,550,'已上架'),
(10003,'联想 YOGA Air 14','轻薄高性能办公本',121,1003,1.31,'2026-03-03 10:00:00','2026-09-15 09:00:00','product-yoga-air14.jpg',500,600,'已上架'),
(10004,'华为 MateBook 14','高分辨率触控轻薄本',121,1002,1.49,'2026-03-04 10:00:00','2026-09-15 09:00:00','product-matebook14.png',500,600,'已上架'),
(10005,'美的 IH智能电饭煲','多段IH加热，智能预约',211,1004,4.20,'2026-03-05 10:00:00','2026-09-15 09:00:00','product-midea-rice.jpg',100,50,'已上架'),
(10006,'苏泊尔远红外电饭煲','远红外穿透加热',211,1005,4.10,'2026-03-06 10:00:00','2026-09-15 09:00:00','product-supor-rice.jpg',100,50,'已上架'),
(10007,'膳魔师真空保温杯','食品级不锈钢内胆',221,1006,0.30,'2026-03-07 10:00:00','2026-09-15 09:00:00','product-thermos-black.jpg',50,30,'已上架'),
(10008,'乐扣乐扣随行保温杯','轻量防漏杯盖',221,1007,0.28,'2026-03-08 10:00:00','2026-09-15 09:00:00','product-locklock-white.jpg',50,20,'已上架'),
(10009,'安踏氮科技跑鞋','缓震回弹日常训练鞋',311,1008,0.62,'2026-03-09 10:00:00','2026-09-15 09:00:00','product-anta-black.jpg',100,50,'已上架'),
(10010,'李宁赤兔跑鞋','轻量透气竞速训练鞋',311,1009,0.58,'2026-03-10 10:00:00','2026-09-15 09:00:00','product-lining-blue.jpg',100,60,'已上架'),
(10011,'欧莱雅玻尿酸洗发水','蓬松清爽，改善毛躁',411,1010,0.75,'2026-03-11 10:00:00','2026-09-15 09:00:00','product-loreal-shampoo.png',30,10,'已上架'),
(10012,'珀莱雅双抗精华','抗氧提亮，细腻肤感',421,1011,0.20,'2026-03-12 10:00:00','2026-09-15 09:00:00','product-proya-serum.png',100,30,'已上架');

INSERT INTO tbl_goods_attr_value(id,spu_id,attr_id,value_type,attr_value) VALUES
(101,10001,1101,1,'6.1英寸'),(102,10001,1102,1,'5G全网通'),(103,10002,1101,1,'6.7英寸'),(104,10002,1102,1,'5G全网通'),
(105,10003,1201,1,'Intel Core Ultra 7'),(106,10003,1202,1,'16GB/32GB'),(107,10004,1201,1,'Intel Core Ultra 5'),(108,10004,1202,1,'16GB/32GB'),
(109,10005,2101,1,'IH电磁加热'),(110,10005,2102,1,'1300W'),(111,10006,2101,1,'远红外加热'),(112,10006,2102,1,'1200W'),
(113,10007,2201,1,'316L不锈钢'),(114,10007,2202,1,'12小时'),(115,10008,2201,1,'304不锈钢'),(116,10008,2202,1,'8小时'),
(117,10009,3101,1,'工程网布'),(118,10009,3102,1,'氮科技中底'),(119,10010,3101,1,'轻量织物'),(120,10010,3102,1,'LIGHT FOAM'),
(121,10011,4101,1,'蓬松控油'),(122,10011,4102,1,'油性及混合发质'),(123,10012,4201,1,'抗氧提亮'),(124,10012,4202,1,'多数肤质');

INSERT INTO tbl_sku(id,spu_id,sku_name,price,stock,sku_img,create_date,update_date,sku_code,sku_status,discount_price) VALUES
(101001,10001,'128GB;黑色',5999.00,0,'product-iphone16.jpg','2026-03-01 10:10:00','2026-09-18 18:00:00','SKU-IP16-128-BLK','启用',5699.00),
(101002,10001,'256GB;黑色',6999.00,0,'product-iphone16.jpg','2026-03-01 10:11:00','2026-09-18 18:00:00','SKU-IP16-256-BLK','启用',6699.00),
(102001,10002,'256GB;黑色',5499.00,0,'product-mate70.png','2026-03-02 10:10:00','2026-09-18 18:00:00','SKU-MATE70-256-BLK','启用',5299.00),
(102002,10002,'512GB;白色',6499.00,0,'product-mate70.png','2026-03-02 10:11:00','2026-09-18 18:00:00','SKU-MATE70-512-WHT','启用',6199.00),
(103001,10003,'16GB+512GB;银色',5999.00,0,'product-yoga-air14.jpg','2026-03-03 10:10:00','2026-09-18 18:00:00','SKU-YOGA14-16-512-SLV','启用',5799.00),
(103002,10003,'32GB+1TB;灰色',7499.00,0,'product-yoga-air14.jpg','2026-03-03 10:11:00','2026-09-18 18:00:00','SKU-YOGA14-32-1T-GRY','启用',7199.00),
(104001,10004,'16GB+512GB;灰色',5999.00,0,'product-matebook14.png','2026-03-04 10:10:00','2026-09-18 18:00:00','SKU-MB14-16-512-GRY','启用',5699.00),
(104002,10004,'32GB+1TB;银色',6999.00,0,'product-matebook14.png','2026-03-04 10:11:00','2026-09-18 18:00:00','SKU-MB14-32-1T-SLV','启用',6699.00),
(105001,10005,'4L;白色',499.00,0,'product-midea-rice.jpg','2026-03-05 10:10:00','2026-09-18 18:00:00','SKU-MIDEA-RICE-4L-WHT','启用',449.00),
(105002,10005,'5L;灰色',599.00,0,'product-midea-rice.jpg','2026-03-05 10:11:00','2026-09-18 18:00:00','SKU-MIDEA-RICE-5L-GRY','启用',549.00),
(106001,10006,'4L;棕色',429.00,0,'product-supor-rice.jpg','2026-03-06 10:10:00','2026-09-18 18:00:00','SKU-SUPOR-RICE-4L-BRN','启用',399.00),
(106002,10006,'5L;金色',529.00,0,'product-supor-rice.jpg','2026-03-06 10:11:00','2026-09-18 18:00:00','SKU-SUPOR-RICE-5L-GLD','启用',479.00),
(107001,10007,'500ml;黑色',299.00,0,'product-thermos-black.jpg','2026-03-07 10:10:00','2026-09-18 18:00:00','SKU-THERMOS-500-BLK','启用',269.00),
(107002,10007,'600ml;蓝色',359.00,0,'product-thermos-black.jpg','2026-03-07 10:11:00','2026-09-18 18:00:00','SKU-THERMOS-600-BLU','启用',329.00),
(108001,10008,'450ml;白色',159.00,0,'product-locklock-white.jpg','2026-03-08 10:10:00','2026-09-18 18:00:00','SKU-LOCK-450-WHT','启用',144.00),
(108002,10008,'600ml;绿色',199.00,0,'product-locklock-white.jpg','2026-03-08 10:11:00','2026-09-18 18:00:00','SKU-LOCK-600-GRN','启用',179.00),
(109001,10009,'42;黑色',499.00,0,'product-anta-black.jpg','2026-03-09 10:10:00','2026-09-18 18:00:00','SKU-ANTA-RUN-42-BLK','启用',449.00),
(109002,10009,'43;白色',499.00,0,'product-anta-black.jpg','2026-03-09 10:11:00','2026-09-18 18:00:00','SKU-ANTA-RUN-43-WHT','启用',449.00),
(110001,10010,'42;白色',599.00,0,'product-lining-blue.jpg','2026-03-10 10:10:00','2026-09-18 18:00:00','SKU-LINING-RUN-42-WHT','启用',549.00),
(110002,10010,'43;蓝色',599.00,0,'product-lining-blue.jpg','2026-03-10 10:11:00','2026-09-18 18:00:00','SKU-LINING-RUN-43-BLU','启用',549.00),
(111001,10011,'500ml;清新花香',89.00,0,'product-loreal-shampoo.png','2026-03-11 10:10:00','2026-09-18 18:00:00','SKU-LOREAL-SHAM-500-FLR','启用',79.00),
(111002,10011,'700ml;淡雅木香',119.00,0,'product-loreal-shampoo.png','2026-03-11 10:11:00','2026-09-18 18:00:00','SKU-LOREAL-SHAM-700-WOD','启用',104.00),
(112001,10012,'30ml;无赠品',299.00,0,'product-proya-serum.png','2026-03-12 10:10:00','2026-09-18 18:00:00','SKU-PROYA-SERUM-30','启用',269.00),
(112002,10012,'50ml;旅行装',429.00,0,'product-proya-serum.png','2026-03-12 10:11:00','2026-09-18 18:00:00','SKU-PROYA-SERUM-50','启用',399.00);

INSERT INTO tbl_sku_sale_attr_value(id,spu_id,sku_id,attr_id,attr_value) VALUES
(1,10001,101001,1103,'128GB'),(2,10001,101001,1104,'黑色'),(3,10001,101002,1103,'256GB'),(4,10001,101002,1104,'黑色'),
(5,10002,102001,1103,'256GB'),(6,10002,102001,1104,'黑色'),(7,10002,102002,1103,'512GB'),(8,10002,102002,1104,'白色'),
(9,10003,103001,1203,'512GB'),(10,10003,103001,1204,'银色'),(11,10003,103002,1203,'1TB'),(12,10003,103002,1204,'灰色'),
(13,10004,104001,1203,'512GB'),(14,10004,104001,1204,'灰色'),(15,10004,104002,1203,'1TB'),(16,10004,104002,1204,'银色'),
(17,10005,105001,2103,'4L'),(18,10005,105001,2104,'白色'),(19,10005,105002,2103,'5L'),(20,10005,105002,2104,'灰色'),
(21,10006,106001,2103,'4L'),(22,10006,106001,2104,'棕色'),(23,10006,106002,2103,'5L'),(24,10006,106002,2104,'金色'),
(25,10007,107001,2203,'500ml'),(26,10007,107001,2204,'黑色'),(27,10007,107002,2203,'600ml'),(28,10007,107002,2204,'蓝色'),
(29,10008,108001,2203,'450ml'),(30,10008,108001,2204,'白色'),(31,10008,108002,2203,'600ml'),(32,10008,108002,2204,'绿色'),
(33,10009,109001,3103,'42'),(34,10009,109001,3104,'黑色'),(35,10009,109002,3103,'43'),(36,10009,109002,3104,'白色'),
(37,10010,110001,3103,'42'),(38,10010,110001,3104,'白色'),(39,10010,110002,3103,'43'),(40,10010,110002,3104,'蓝色'),
(41,10011,111001,4103,'500ml'),(42,10011,111001,4104,'清新花香'),(43,10011,111002,4103,'700ml'),(44,10011,111002,4104,'淡雅木香'),
(45,10012,112001,4203,'30ml'),(46,10012,112001,4204,'无赠品'),(47,10012,112002,4203,'50ml'),(48,10012,112002,4204,'旅行装');

-- Members. Current spend and points are recalculated after orders are inserted.
INSERT INTO tbl_member(id,user_id,nickname,phone,member_level,source,balance,points,total_spent,status,registered_at) VALUES
(1,'U10001','林晓','13900002001','普通会员','APP',120.00,100,0,'正常','2026-02-12 09:10:00'),(2,'U10002','陈航','13900002002','普通会员','小程序',80.00,100,0,'正常','2026-02-18 14:22:00'),
(3,'U10003','周宁','13900002003','普通会员','APP',35.00,100,0,'正常','2026-03-09 11:45:00'),(4,'U10004','沈悦','13900002004','普通会员','PC网页',0.00,100,0,'正常','2026-03-21 16:30:00'),
(5,'U10005','顾安','13900002005','普通会员','APP',60.00,100,0,'正常','2026-04-03 08:15:00'),(6,'U10006','许言','13900002006','普通会员','小程序',20.00,100,0,'正常','2026-04-15 10:20:00'),
(7,'U10007','唐婧','13900002007','普通会员','APP',0.00,100,0,'正常','2026-04-28 13:05:00'),(8,'U10008','陆川','13900002008','普通会员','PC网页',50.00,100,0,'正常','2026-05-06 09:50:00'),
(9,'U10009','苏禾','13900002009','普通会员','APP',30.00,100,0,'正常','2026-05-19 17:40:00'),(10,'U10010','叶舟','13900002010','普通会员','小程序',10.00,100,0,'正常','2026-05-28 12:30:00'),
(11,'U10011','宋妍','13900002011','普通会员','APP',0.00,100,0,'正常','2026-06-02 10:35:00'),(12,'U10012','韩旭','13900002012','普通会员','PC网页',90.00,100,0,'正常','2026-06-10 18:10:00'),
(13,'U10013','冯琳','13900002013','普通会员','APP',25.00,100,0,'正常','2026-06-18 08:40:00'),(14,'U10014','何然','13900002014','普通会员','小程序',0.00,100,0,'正常','2026-06-26 15:25:00'),
(15,'U10015','邱月','13900002015','普通会员','APP',40.00,100,0,'正常','2026-07-03 11:15:00'),(16,'U10016','程野','13900002016','普通会员','PC网页',0.00,100,0,'正常','2026-07-08 19:20:00'),
(17,'U10017','罗伊','13900002017','普通会员','APP',100.00,100,0,'正常','2026-07-15 09:05:00'),(18,'U10018','姜寻','13900002018','普通会员','小程序',20.00,100,0,'正常','2026-07-22 14:35:00'),
(19,'U10019','魏岚','13900002019','普通会员','APP',0.00,100,0,'正常','2026-07-29 16:45:00'),(20,'U10020','钟毅','13900002020','普通会员','PC网页',15.00,100,0,'正常','2026-08-04 10:05:00'),
(21,'U10021','方晨','13900002021','普通会员','APP',0.00,100,0,'正常','2026-08-09 12:15:00'),(22,'U10022','袁清','13900002022','普通会员','小程序',60.00,100,0,'正常','2026-08-15 15:50:00'),
(23,'U10023','彭然','13900002023','普通会员','APP',0.00,100,0,'正常','2026-08-22 09:30:00'),(24,'U10024','杜若','13900002024','普通会员','PC网页',30.00,100,0,'黑名单','2026-08-28 18:00:00');

-- Marketing activities and placements. Product names and target URLs point to the same catalog above.
INSERT INTO tbl_activity(id,activity_name,activity_type,discount_value,start_time,end_time,total_count,claimed_count,status,product_name,created_at) VALUES
(1,'数码焕新季','满减',300.00,'2026-09-01 00:00:00','2026-09-30 23:59:59',500,0,'进行中','Apple iPhone 16','2026-08-25 10:00:00'),
(2,'电脑开学季','满减',300.00,'2026-09-01 00:00:00','2026-09-25 23:59:59',300,0,'进行中','联想 YOGA Air 14','2026-08-25 10:10:00'),
(3,'品质厨电周','满减',50.00,'2026-09-05 00:00:00','2026-09-22 23:59:59',800,0,'进行中','美的 IH智能电饭煲','2026-08-28 09:00:00'),
(4,'秋季运动节','满减',50.00,'2026-09-01 00:00:00','2026-09-20 23:59:59',600,0,'进行中','安踏氮科技跑鞋','2026-08-28 09:10:00'),
(5,'洗护焕新周','满减',20.00,'2026-09-10 00:00:00','2026-09-24 23:59:59',1000,0,'进行中','欧莱雅玻尿酸洗发水','2026-09-03 11:00:00'),
(6,'新会员专享券','优惠券',30.00,'2026-09-01 00:00:00','2026-09-30 23:59:59',2000,0,'进行中','珀莱雅双抗精华','2026-08-25 11:00:00');

INSERT INTO tbl_advertisement(id,ad_name,placement,image_url,target_url,weight,start_time,end_time,status,created_at) VALUES
(1,'iPhone 16 焕新主视觉','首页轮播','/PublishGoods/showImg/goods/product-iphone16.jpg','/goods/10001?sku=SKU-IP16-256-BLK',100,'2026-09-01 00:00:00','2026-09-30 23:59:59','已上架','2026-08-25 14:00:00'),
(2,'Mate 70 旗舰推荐','首页轮播','/PublishGoods/showImg/goods/product-mate70.png','/goods/10002?sku=SKU-MATE70-256-BLK',90,'2026-09-01 00:00:00','2026-09-30 23:59:59','已上架','2026-08-25 14:10:00'),
(3,'开学轻薄本专场','频道横幅','/PublishGoods/showImg/goods/product-yoga-air14.jpg','/goods/10003?sku=SKU-YOGA14-16-512-SLV',80,'2026-09-01 00:00:00','2026-09-25 23:59:59','已上架','2026-08-26 09:00:00'),
(4,'品质厨电周','频道横幅','/PublishGoods/showImg/goods/product-midea-rice.jpg','/goods/10005?sku=SKU-MIDEA-RICE-4L-WHT',70,'2026-09-05 00:00:00','2026-09-22 23:59:59','已上架','2026-08-28 10:00:00'),
(5,'秋季跑步装备','搜索推荐','/PublishGoods/showImg/goods/product-anta-black.jpg','/goods/10009?sku=SKU-ANTA-RUN-42-BLK',60,'2026-09-01 00:00:00','2026-09-20 23:59:59','已上架','2026-08-28 10:10:00'),
(6,'双抗精华新品','首页轮播','/PublishGoods/showImg/goods/product-proya-serum.png','/goods/10012?sku=SKU-PROYA-SERUM-30',50,'2026-09-20 00:00:00','2026-10-10 23:59:59','草稿','2026-09-15 16:00:00');

-- Orders cover every supported status plus refund, return/refund and exchange evidence.
INSERT INTO tbl_sales_order(order_id,user_id,product_id,product_name,category,order_status,pay_amount,quantity,coupon_amount,order_channel,order_time,pay_time,delivery_time,complete_time,is_return,return_type,return_reason,return_amount,return_time,remark) VALUES
('O202609010001','U10001','SKU-IP16-256-BLK','Apple iPhone 16 256GB 黑色','5G手机','已完成',6699.00,1,300.00,'APP','2026-09-01 20:16:00','2026-09-01 20:17:00','2026-09-02 10:30:00','2026-09-04 15:20:00',0,NULL,NULL,NULL,NULL,'ACT-1 数码焕新季'),
('O202609020001','U10002','SKU-MATE70-256-BLK','华为 Mate 70 256GB 黑色','5G手机','已完成',5299.00,1,200.00,'小程序','2026-09-02 11:05:00','2026-09-02 11:06:00','2026-09-03 09:40:00','2026-09-05 18:10:00',0,NULL,NULL,NULL,NULL,'ACT-1 数码焕新季'),
('O202609030001','U10003','SKU-YOGA14-16-512-SLV','联想 YOGA Air 14 16GB+512GB 银色','轻薄笔记本','已完成',5799.00,1,200.00,'PC网页','2026-09-03 14:20:00','2026-09-03 14:22:00','2026-09-04 11:15:00','2026-09-06 16:30:00',0,NULL,NULL,NULL,NULL,'ACT-2 电脑开学季'),
('O202609040001','U10004','SKU-MIDEA-RICE-4L-WHT','美的 IH智能电饭煲 4L 白色','电饭煲','已完成',449.00,1,50.00,'APP','2026-09-04 09:45:00','2026-09-04 09:46:00','2026-09-04 17:00:00','2026-09-06 12:10:00',0,NULL,NULL,NULL,NULL,'ACT-3 品质厨电周'),
('O202609050001','U10005','SKU-THERMOS-500-BLK','膳魔师真空保温杯 500ml 黑色','保温杯','已完成',548.00,2,50.00,'小程序','2026-09-05 18:02:00','2026-09-05 18:03:00','2026-09-06 10:20:00','2026-09-08 14:00:00',0,NULL,NULL,NULL,NULL,'日用满减券'),
('O202609060001','U10006','SKU-ANTA-RUN-42-BLK','安踏氮科技跑鞋 42 黑色','跑步鞋','已完成',449.00,1,50.00,'APP','2026-09-06 08:30:00','2026-09-06 08:31:00','2026-09-06 16:30:00','2026-09-08 20:05:00',0,NULL,NULL,NULL,NULL,'ACT-4 秋季运动节'),
('O202609070001','U10007','SKU-LOREAL-SHAM-500-FLR','欧莱雅玻尿酸洗发水 500ml','洗发护发','已完成',158.00,2,20.00,'APP','2026-09-07 13:12:00','2026-09-07 13:13:00','2026-09-08 09:20:00','2026-09-10 11:40:00',0,NULL,NULL,NULL,NULL,'ACT-5 洗护焕新周'),
('O202609080001','U10008','SKU-PROYA-SERUM-30','珀莱雅双抗精华 30ml','面部精华','已完成',269.00,1,30.00,'PC网页','2026-09-08 19:20:00','2026-09-08 19:22:00','2026-09-09 10:10:00','2026-09-11 17:30:00',0,NULL,NULL,NULL,NULL,'ACT-6 新会员专享券'),
('O202609090001','U10009','SKU-LOCK-450-WHT','乐扣乐扣随行保温杯 450ml 白色','保温杯','已完成',288.00,2,30.00,'APP','2026-09-09 10:05:00','2026-09-09 10:06:00','2026-09-09 17:45:00','2026-09-12 09:25:00',0,NULL,NULL,NULL,NULL,'日用满减券'),
('O202609100001','U10010','SKU-LINING-RUN-43-BLU','李宁赤兔跑鞋 43 蓝色','跑步鞋','已完成',549.00,1,50.00,'小程序','2026-09-10 16:40:00','2026-09-10 16:41:00','2026-09-11 09:35:00','2026-09-13 13:15:00',0,NULL,NULL,NULL,NULL,'ACT-4 秋季运动节'),
('O202609110001','U10011','SKU-SUPOR-RICE-4L-BRN','苏泊尔远红外电饭煲 4L 棕色','电饭煲','已完成',399.00,1,30.00,'APP','2026-09-11 09:18:00','2026-09-11 09:19:00','2026-09-11 17:30:00','2026-09-14 10:20:00',1,'退货退款','收到商品后发现内胆有磕碰',399.00,'2026-09-16 15:30:00','售后审核通过并完成退货入库'),
('O202609120001','U10012','SKU-MATE70-512-WHT','华为 Mate 70 512GB 白色','5G手机','已完成',6199.00,1,300.00,'PC网页','2026-09-12 12:30:00','2026-09-12 12:31:00','2026-09-13 10:00:00','2026-09-15 18:20:00',0,NULL,NULL,NULL,NULL,'ACT-1 数码焕新季'),
('O202609130001','U10013','SKU-IP16-128-BLK','Apple iPhone 16 128GB 黑色','5G手机','待付款',5699.00,1,300.00,'APP','2026-09-13 20:10:00',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,'ACT-1 数码焕新季'),
('O202609140001','U10014','SKU-MIDEA-RICE-5L-GRY','美的 IH智能电饭煲 5L 灰色','电饭煲','待付款',549.00,1,50.00,'小程序','2026-09-14 09:15:00',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,'ACT-3 品质厨电周'),
('O202609150001','U10015','SKU-PROYA-SERUM-50','珀莱雅双抗精华 50ml','面部精华','待付款',399.00,1,30.00,'APP','2026-09-15 10:25:00',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,'ACT-6 新会员专享券'),
('O202609150002','U10016','SKU-THERMOS-600-BLU','膳魔师真空保温杯 600ml 蓝色','保温杯','待付款',329.00,1,30.00,'PC网页','2026-09-15 15:40:00',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,'日用满减券'),
('O202609160001','U10017','SKU-YOGA14-32-1T-GRY','联想 YOGA Air 14 32GB+1TB 灰色','轻薄笔记本','待发货',7199.00,1,300.00,'APP','2026-09-16 08:35:00','2026-09-16 08:36:00',NULL,NULL,0,NULL,NULL,NULL,NULL,'ACT-2 电脑开学季'),
('O202609160002','U10018','SKU-MB14-16-512-GRY','华为 MateBook 14 16GB+512GB 灰色','轻薄笔记本','待发货',5699.00,1,300.00,'小程序','2026-09-16 10:20:00','2026-09-16 10:21:00',NULL,NULL,0,NULL,NULL,NULL,NULL,'ACT-2 电脑开学季'),
('O202609160003','U10019','SKU-ANTA-RUN-43-WHT','安踏氮科技跑鞋 43 白色','跑步鞋','待发货',449.00,1,50.00,'APP','2026-09-16 12:42:00','2026-09-16 12:43:00',NULL,NULL,0,NULL,NULL,NULL,NULL,'ACT-4 秋季运动节'),
('O202609160004','U10020','SKU-LOREAL-SHAM-700-WOD','欧莱雅玻尿酸洗发水 700ml','洗发护发','待发货',208.00,2,30.00,'PC网页','2026-09-16 17:10:00','2026-09-16 17:11:00',NULL,NULL,0,NULL,NULL,NULL,NULL,'ACT-5 洗护焕新周'),
('O202609170001','U10021','SKU-LOCK-600-GRN','乐扣乐扣随行保温杯 600ml 绿色','保温杯','待发货',358.00,2,40.00,'APP','2026-09-17 09:05:00','2026-09-17 09:06:00',NULL,NULL,0,NULL,NULL,NULL,NULL,'日用满减券'),
('O202609170002','U10022','SKU-IP16-128-BLK','Apple iPhone 16 128GB 黑色','5G手机','已发货',5699.00,1,300.00,'小程序','2026-09-17 10:30:00','2026-09-17 10:31:00','2026-09-18 09:15:00',NULL,0,NULL,NULL,NULL,NULL,'ACT-1 数码焕新季'),
('O202609170003','U10023','SKU-SUPOR-RICE-5L-GLD','苏泊尔远红外电饭煲 5L 金色','电饭煲','已发货',479.00,1,50.00,'APP','2026-09-17 11:20:00','2026-09-17 11:21:00','2026-09-18 10:00:00',NULL,0,NULL,NULL,NULL,NULL,'ACT-3 品质厨电周'),
('O202609170004','U10024','SKU-PROYA-SERUM-30','珀莱雅双抗精华 30ml','面部精华','已发货',269.00,1,30.00,'PC网页','2026-09-17 14:10:00','2026-09-17 14:12:00','2026-09-18 11:20:00',NULL,0,NULL,NULL,NULL,NULL,'ACT-6 新会员专享券'),
('O202609170005','U10001','SKU-LINING-RUN-42-WHT','李宁赤兔跑鞋 42 白色','跑步鞋','已发货',549.00,1,50.00,'APP','2026-09-17 18:25:00','2026-09-17 18:26:00','2026-09-18 14:00:00',NULL,0,NULL,NULL,NULL,NULL,'ACT-4 秋季运动节'),
('O202609180001','U10002','SKU-MB14-32-1T-SLV','华为 MateBook 14 32GB+1TB 银色','轻薄笔记本','已取消',6699.00,1,300.00,'APP','2026-09-18 08:05:00',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,'用户在支付前取消'),
('O202609180002','U10003','SKU-THERMOS-500-BLK','膳魔师真空保温杯 500ml 黑色','保温杯','已取消',269.00,1,30.00,'小程序','2026-09-18 09:20:00',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,'用户选错颜色'),
('O202609180003','U10004','SKU-LOREAL-SHAM-500-FLR','欧莱雅玻尿酸洗发水 500ml','洗发护发','已取消',79.00,1,10.00,'APP','2026-09-18 10:45:00',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,'重复下单'),
('O202609120002','U10005','SKU-ANTA-RUN-42-BLK','安踏氮科技跑鞋 42 黑色','跑步鞋','已完成',449.00,1,50.00,'APP','2026-09-12 19:05:00','2026-09-12 19:06:00','2026-09-13 09:10:00','2026-09-15 12:00:00',1,'退款','物流延迟补偿',50.00,'2026-09-16 10:00:00','保留商品，部分退款50元'),
('O202609130002','U10006','SKU-MIDEA-RICE-4L-WHT','美的 IH智能电饭煲 4L 白色','电饭煲','已完成',449.00,1,50.00,'小程序','2026-09-13 16:25:00','2026-09-13 16:26:00','2026-09-14 09:40:00','2026-09-16 11:30:00',1,'换货','外包装运输破损',0.00,'2026-09-17 14:20:00','完成同款换货，库存净变化为0');

-- Activity usage is derived from paid order evidence in the order remarks.
UPDATE tbl_activity a SET claimed_count=(SELECT COUNT(*) FROM tbl_sales_order o WHERE o.remark LIKE CONCAT('%ACT-',a.id,'%') AND o.pay_time IS NOT NULL);

-- Inventory starts at 100 per SKU, then reconciles shipped/completed sales and returned stock.
INSERT INTO tbl_inventory(id,sku_code,product_name,warehouse,stock,locked_stock,warn_stock,cost_price,status,updated_at)
SELECT s.id,s.sku_code,CONCAT(p.goods_name,' ',REPLACE(s.sku_name,';',' ')),
       CASE WHEN p.id IN (10001,10005,10009) THEN '广州中心仓'
            WHEN p.id IN (10003,10007,10011) THEN '华东上海仓'
            WHEN p.id IN (10004,10008,10012) THEN '华北北京仓'
            WHEN p.id IN (10002,10006) THEN '西南成都仓'
            ELSE '华中武汉仓' END,100,0,
       CASE WHEN s.price>=5000 THEN 10 WHEN s.price>=500 THEN 15 ELSE 20 END,
       ROUND(s.price*0.72,2),1,'2026-09-18 18:30:00'
FROM tbl_sku s JOIN tbl_spu_detail p ON p.id=s.spu_id;

UPDATE tbl_inventory i
LEFT JOIN (
  SELECT product_id,
         SUM(CASE WHEN order_status IN ('已发货','已完成') THEN quantity ELSE 0 END) shipped_qty,
         SUM(CASE WHEN is_return=1 AND return_type='退货退款' THEN quantity ELSE 0 END) returned_qty,
         SUM(CASE WHEN order_status IN ('待付款','待发货') THEN quantity ELSE 0 END) locked_qty
  FROM tbl_sales_order GROUP BY product_id
) x ON x.product_id=i.sku_code
SET i.stock=100-COALESCE(x.shipped_qty,0)+COALESCE(x.returned_qty,0),i.locked_stock=COALESCE(x.locked_qty,0);

UPDATE tbl_sku s JOIN tbl_inventory i ON i.sku_code=s.sku_code SET s.stock=i.stock,s.update_date='2026-09-18 18:30:00';

INSERT INTO tbl_inventory_flow(inventory_id,flow_no,change_qty,before_qty,after_qty,biz_type,remark,created_at)
SELECT id,CONCAT('INIT-',sku_code),100,0,100,'采购入库','2026年9月期初采购入库','2026-09-01 08:00:00' FROM tbl_inventory;

INSERT INTO tbl_inventory_flow(inventory_id,flow_no,change_qty,before_qty,after_qty,biz_type,remark,created_at)
SELECT i.id,CONCAT('SALE-',i.sku_code),-x.shipped_qty,100,100-x.shipped_qty,'销售出库','已发货及已完成订单汇总出库','2026-09-18 18:00:00'
FROM tbl_inventory i JOIN (
  SELECT product_id,SUM(quantity) shipped_qty FROM tbl_sales_order WHERE order_status IN ('已发货','已完成') GROUP BY product_id
) x ON x.product_id=i.sku_code;

INSERT INTO tbl_inventory_flow(inventory_id,flow_no,change_qty,before_qty,after_qty,biz_type,remark,created_at)
SELECT i.id,CONCAT('RETURN-',i.sku_code),x.returned_qty,100-x.shipped_qty,100-x.shipped_qty+x.returned_qty,'退货入库','退货退款订单验收入库','2026-09-16 16:00:00'
FROM tbl_inventory i JOIN (
  SELECT product_id,SUM(CASE WHEN order_status IN ('已发货','已完成') THEN quantity ELSE 0 END) shipped_qty,
         SUM(CASE WHEN is_return=1 AND return_type='退货退款' THEN quantity ELSE 0 END) returned_qty
  FROM tbl_sales_order GROUP BY product_id HAVING returned_qty>0
) x ON x.product_id=i.sku_code;

-- Member spend and points are calculated from completed net revenue.
UPDATE tbl_member m LEFT JOIN (
  SELECT user_id,ROUND(SUM(pay_amount-COALESCE(return_amount,0)),2) spent
  FROM tbl_sales_order WHERE order_status='已完成' GROUP BY user_id
) x ON x.user_id=m.user_id
SET m.total_spent=COALESCE(x.spent,0),m.points=100+FLOOR(COALESCE(x.spent,0)/10),
    m.member_level=CASE WHEN COALESCE(x.spent,0)>=5000 THEN '黄金会员' WHEN COALESCE(x.spent,0)>=1000 THEN 'VIP会员' ELSE '普通会员' END;

INSERT INTO tbl_points_flow(member_id,change_points,before_points,after_points,biz_type,remark,created_at)
SELECT id,100,0,100,'注册赠送','新会员注册赠送积分',registered_at FROM tbl_member;

INSERT INTO tbl_points_flow(member_id,change_points,before_points,after_points,biz_type,remark,created_at)
SELECT id,points-100,100,points,'订单累计','已完成订单净消费积分汇总','2026-09-18 18:30:00' FROM tbl_member WHERE points>100;

-- Behavior evidence is generated from the exact same order/member/SKU records.
INSERT INTO tbl_behavior_event(log_id,user_id,product_id,behavior_type,source,search_keyword,session_id,device_type,ip_city,product_price,quantity,behavior_time)
SELECT 100000+ROW_NUMBER() OVER(ORDER BY order_time,order_id),user_id,product_id,'浏览',order_channel,product_name,
       CONCAT('SESS-',order_id),CASE WHEN order_channel='PC网页' THEN '电脑' ELSE '手机' END,
       CASE MOD(CAST(SUBSTRING(user_id,2) AS UNSIGNED),4) WHEN 0 THEN '广州' WHEN 1 THEN '深圳' WHEN 2 THEN '杭州' ELSE '上海' END,
       ROUND((pay_amount+coupon_amount)/quantity,2),quantity,DATE_SUB(order_time,INTERVAL 25 MINUTE)
FROM tbl_sales_order;

INSERT INTO tbl_behavior_event(log_id,user_id,product_id,behavior_type,source,search_keyword,session_id,device_type,ip_city,product_price,quantity,behavior_time)
SELECT 200000+ROW_NUMBER() OVER(ORDER BY order_time,order_id),user_id,product_id,'加入购物车',order_channel,NULL,
       CONCAT('SESS-',order_id),CASE WHEN order_channel='PC网页' THEN '电脑' ELSE '手机' END,
       CASE MOD(CAST(SUBSTRING(user_id,2) AS UNSIGNED),4) WHEN 0 THEN '广州' WHEN 1 THEN '深圳' WHEN 2 THEN '杭州' ELSE '上海' END,
       ROUND((pay_amount+coupon_amount)/quantity,2),quantity,DATE_SUB(order_time,INTERVAL 8 MINUTE)
FROM tbl_sales_order;

INSERT INTO tbl_behavior_event(log_id,user_id,product_id,behavior_type,source,search_keyword,session_id,device_type,ip_city,product_price,quantity,behavior_time)
SELECT 300000+ROW_NUMBER() OVER(ORDER BY pay_time,order_id),user_id,product_id,'购买',order_channel,NULL,
       CONCAT('SESS-',order_id),CASE WHEN order_channel='PC网页' THEN '电脑' ELSE '手机' END,
       CASE MOD(CAST(SUBSTRING(user_id,2) AS UNSIGNED),4) WHEN 0 THEN '广州' WHEN 1 THEN '深圳' WHEN 2 THEN '杭州' ELSE '上海' END,
       ROUND((pay_amount+coupon_amount)/quantity,2),quantity,pay_time
FROM tbl_sales_order WHERE pay_time IS NOT NULL;

INSERT INTO tbl_behavior_event(log_id,user_id,product_id,behavior_type,source,search_keyword,session_id,device_type,ip_city,product_price,quantity,behavior_time)
SELECT 400000+ROW_NUMBER() OVER(ORDER BY order_time,order_id),user_id,product_id,'收藏',order_channel,NULL,
       CONCAT('FAV-',order_id),CASE WHEN order_channel='PC网页' THEN '电脑' ELSE '手机' END,'广州',
       ROUND((pay_amount+coupon_amount)/quantity,2),1,DATE_SUB(order_time,INTERVAL 1 DAY)
FROM tbl_sales_order WHERE MOD(CAST(RIGHT(order_id,2) AS UNSIGNED),3)=1;

-- Reviews exist only for completed, non-returned orders.
INSERT INTO tbl_review(review_id,order_id,user_id,product_id,rating,review_content,review_time,is_append,append_content,image_count,praise_num,merchant_reply,merchant_reply_time)
SELECT CONCAT('R',SUBSTRING(order_id,2)),order_id,user_id,product_id,
       CASE MOD(CAST(RIGHT(order_id,2) AS UNSIGNED),5) WHEN 0 THEN 4 ELSE 5 END,
       CASE category WHEN '5G手机' THEN '运行流畅，拍照和续航符合预期。' WHEN '轻薄笔记本' THEN '办公性能充足，机身轻便，屏幕清晰。'
         WHEN '电饭煲' THEN '预约方便，米饭口感不错，清洁也简单。' WHEN '保温杯' THEN '密封良好，保温时间符合描述。'
         WHEN '跑步鞋' THEN '尺码合适，缓震明显，日常训练很舒服。' WHEN '洗发护发' THEN '泡沫细腻，洗后清爽，香味不浓。'
         ELSE '包装完好，使用感受与商品描述一致。' END,
       DATE_ADD(complete_time,INTERVAL 1 DAY),MOD(CAST(RIGHT(order_id,2) AS UNSIGNED),4)=0,
       CASE WHEN MOD(CAST(RIGHT(order_id,2) AS UNSIGNED),4)=0 THEN '使用一周后状态稳定，会继续回购。' ELSE NULL END,
       MOD(CAST(RIGHT(order_id,2) AS UNSIGNED),3),MOD(CAST(RIGHT(order_id,2) AS UNSIGNED),12),
       '感谢您的真实反馈，我们会继续做好商品与履约服务。',DATE_ADD(complete_time,INTERVAL 1 DAY)+INTERVAL 2 HOUR
FROM tbl_sales_order WHERE order_status='已完成' AND is_return=0;

-- Service sessions use the same members; order numbers are recorded in the conversation identity.
INSERT INTO tbl_customer_service_session(session_id,user_id,merchant_id,intent_type,first_response_seconds,is_resolved,message_count,started_at) VALUES
('CS-O202609010001','U10001','MALL001','发票咨询',18,1,6,'2026-09-04 16:10:00'),
('CS-O202609020001','U10002','MALL001','物流咨询',25,1,8,'2026-09-03 15:20:00'),
('CS-O202609030001','U10003','MALL001','商品咨询',12,1,5,'2026-09-03 13:50:00'),
('CS-O202609050001','U10005','MALL001','售后政策',30,1,7,'2026-09-08 15:30:00'),
('CS-O202609060001','U10006','MALL001','尺码咨询',15,1,6,'2026-09-06 08:05:00'),
('CS-O202609080001','U10008','MALL001','使用咨询',42,1,9,'2026-09-12 10:20:00'),
('CS-O202609110001','U10011','MALL001','退货退款',55,1,14,'2026-09-14 11:05:00'),
('CS-O202609130001','U10013','MALL001','支付咨询',20,0,4,'2026-09-13 20:20:00'),
('CS-O202609160001','U10017','MALL001','发货咨询',35,1,6,'2026-09-17 09:40:00'),
('CS-O202609170002','U10022','MALL001','物流咨询',28,1,7,'2026-09-18 16:20:00'),
('CS-O202609120002','U10005','MALL001','退款进度',40,1,10,'2026-09-16 09:20:00'),
('CS-O202609130002','U10006','MALL001','换货处理',48,1,12,'2026-09-17 13:50:00');

SET FOREIGN_KEY_CHECKS = 1;

-- Reconciliation output: every result should be zero except the row-count checks.
SELECT 'unknown_order_sku' check_name,COUNT(*) issue_count
FROM tbl_sales_order o LEFT JOIN tbl_inventory i ON i.sku_code=o.product_id WHERE i.id IS NULL
UNION ALL SELECT 'unknown_behavior_member',COUNT(*) FROM tbl_behavior_event b LEFT JOIN tbl_member m ON m.user_id=b.user_id WHERE m.id IS NULL
UNION ALL SELECT 'unknown_behavior_sku',COUNT(*) FROM tbl_behavior_event b LEFT JOIN tbl_inventory i ON i.sku_code=b.product_id WHERE i.id IS NULL
UNION ALL SELECT 'review_without_completed_order',COUNT(*) FROM tbl_review r LEFT JOIN tbl_sales_order o ON o.order_id=r.order_id AND o.order_status='已完成' WHERE o.order_id IS NULL
UNION ALL SELECT 'sku_inventory_stock_mismatch',COUNT(*) FROM tbl_sku s JOIN tbl_inventory i ON i.sku_code=s.sku_code WHERE s.stock<>i.stock
UNION ALL SELECT 'member_spend_mismatch',COUNT(*) FROM tbl_member m LEFT JOIN (SELECT user_id,ROUND(SUM(pay_amount-COALESCE(return_amount,0)),2) spent FROM tbl_sales_order WHERE order_status='已完成' GROUP BY user_id) x ON x.user_id=m.user_id WHERE m.total_spent<>COALESCE(x.spent,0);

SELECT 'categories' item,COUNT(*) total FROM tbl_goods_category
UNION ALL SELECT 'brands',COUNT(*) FROM tbl_brand
UNION ALL SELECT 'spus',COUNT(*) FROM tbl_spu_detail
UNION ALL SELECT 'skus',COUNT(*) FROM tbl_sku
UNION ALL SELECT 'members',COUNT(*) FROM tbl_member
UNION ALL SELECT 'orders',COUNT(*) FROM tbl_sales_order
UNION ALL SELECT 'inventory_flows',COUNT(*) FROM tbl_inventory_flow
UNION ALL SELECT 'points_flows',COUNT(*) FROM tbl_points_flow
UNION ALL SELECT 'behavior_events',COUNT(*) FROM tbl_behavior_event
UNION ALL SELECT 'reviews',COUNT(*) FROM tbl_review
UNION ALL SELECT 'service_sessions',COUNT(*) FROM tbl_customer_service_session;

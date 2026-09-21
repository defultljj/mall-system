# 电商数据管理平台

一个基于 Java Spring Boot 的电商后台数据管理系统，覆盖商品、SKU、库存、订单、会员、营销和经营分析等常用后台场景。

## 项目特点

- 商品发布、编辑、主图管理、规格参数和销售属性管理
- SKU 价格、库存、状态及多仓库存管理
- 订单查询、状态流转和售后记录
- 会员、积分、营销活动和广告管理
- ECharts 经营概览和业务数据分析
- Redis 发布流程暂存与缓存
- 可选接入 Hadoop HDFS + Hive，完成用户行为、销售、评价和客服数据的清洗与分析

## 技术栈

Java 17、Spring Boot 2.6、MyBatis-Plus、MySQL 8、Redis、Hadoop HDFS、Apache Hive、ECharts、HTML/CSS/JavaScript。

## 快速开始

1. 创建 MySQL 数据库 `mall_sys`，执行 `db/mall_sys_full_demo.sql` 初始化示例数据。
2. 复制 `src/main/resources/application.example.properties` 为 `application.properties`，填写 MySQL、Redis 和可选 Hive 配置。
3. 启动 Redis（默认 `localhost:6379`）。
4. 使用 JDK 17+ 启动：

   ```bash
   mvn spring-boot:run
   ```

   或先打包后运行：

   ```bash
   mvn -DskipTests package
   java -jar target/mall-sys-1.0.jar
   ```

5. 浏览器访问 `http://localhost:8080/`。

默认演示账号：`admin` / `123456`。正式使用时请修改密码和本地配置。

## 目录说明

| 目录 | 说明 |
| --- | --- |
| `src/main/java` | Spring Boot 后端代码、控制器、服务和数据访问层 |
| `src/main/resources/static` | 管理后台前端页面、样式和脚本 |
| `db` | 数据库初始化脚本和阶段性迁移脚本 |
| `upload` | 商品、品牌和相册图片资源 |
| `SECOND-PHASE-IMPLEMENTATION-PLAN.md` | Hadoop/Hive 第二阶段实施方案 |

## 说明

本项目定位为课程设计和功能演示系统，数据为完整的本地演示数据，不建议直接用于生产环境。Hadoop/Hive 为可选分析链路，管理后台在 Hive 暂时不可用时仍可启动。


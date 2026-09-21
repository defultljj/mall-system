# 第二阶段电商数据处理与分析实施方案

## 1. 目标

严格落地《第二阶段-电商用户行为、销售数据处理与分析详细需求文档》，把现有商城业务数据和用户行为、评价、客服样本数据串成一条可重复运行的链路：

```text
原始数据采集 → HDFS ODS → Hive DWD清洗 → Hive ADS指标
→ MySQL指标落地 → Spring Boot查询接口 → Vue 3 + ECharts看板
```

本项目仍是本地课程项目，不接入真实支付、物流或第三方客服，但清洗规则、指标口径、业务关联和接口行为按需求文档实现。

## 2. 当前缺口

### 2.1 已有基础

- 商品、SKU、品类、会员、订单、库存、活动和权限基础业务模块。
- 统一的商城演示数据和订单、库存、会员积分联动。
- 虚拟机 Hadoop 3.1.0、HDFS、Yarn、Hive 3.1.2。
- Hive Metastore 已使用 MySQL，HiveServer2 可通过 `192.168.28.150:10000` 连接。
- Spring Boot 原有管理后台和四个分析入口。

### 2.2 必须补齐

- 原始日志、订单、评价、客服数据进入 HDFS 的 ODS 层。
- Hive DWD 清洗明细层和 ADS 指标层。
- 用户行为完整路径、搜索、入口、分层和时段指标。
- 销售趋势、订单流失、支付、退换货和客单价指标。
- 评价有效性、情感、关键词、品类口碑和追评指标。
- 客服消息明细、咨询分类、会话时长和咨询转化指标。
- 四类 MySQL 指标表、批次、质量和同步记录。
- 支持时间/维度筛选、分页、导出、下钻的 Spring Boot 接口。
- Vue 3 + ECharts 分析中心、漏斗图、热力图、词云图和趋势图。
- 每日凌晨批处理、手动重跑、失败记录和数据对账。

## 3. 环境边界

| 组件 | 位置 | 用途 |
|---|---|---|
| MySQL `mall_sys` | Windows | 商城业务和分析结果库 |
| Redis | Windows | 现有发布草稿和业务缓存 |
| Hadoop/HDFS/Yarn | Linux虚拟机 `192.168.28.150` | 原始数据和Hive存储计算 |
| Hive Metastore | Linux虚拟机，9083 | Hive元数据，元数据库为MySQL `hive` |
| HiveServer2 | Linux虚拟机，10000 | Spring Boot/Beeline查询入口 |
| Spring Boot | Windows | 业务、ETL编排和分析API |
| Vue 3 + ECharts | Windows构建后由Spring Boot提供 | 分析中心 |

不使用 Docker。虚拟机采用单机伪分布式模式，适合本地样本和课程项目。

## 4. 数据分层设计

### 4.1 ODS 原始层

建立以下Hive表，保留原始字段和采集批次：

- `ods_user_behavior_log`
- `ods_sales_order`
- `ods_order_item`
- `ods_after_sale`
- `ods_user_review`
- `ods_service_message`
- `ods_member`
- `ods_product`
- `ods_category`

每条数据保留 `batch_date`、`source_file`、`ingest_time`、`row_number`，原始层不覆盖。

### 4.2 DWD 清洗明细层

- `dwd_user_behavior_event`
- `dwd_user_session_path`
- `dwd_sales_order_item`
- `dwd_after_sale_detail`
- `dwd_review_detail`
- `dwd_service_message`
- `dwd_service_session`

清洗层统一用户、商品、SKU、品类、会员层级、设备、渠道、日期和小时维度，并记录拒绝原因。

### 4.3 ADS 指标层

四个主题指标表：

- `ads_behavior_metric_daily`
- `ads_sales_metric_daily`
- `ads_review_metric_daily`
- `ads_service_metric_daily`

通用字段：`stat_date`、`period_type`、`metric_code`、`metric_name`、`metric_value`、`dimension_type`、`dimension_value`、`rank_no`、`batch_no`、`calculated_at`。

### 4.4 MySQL落地表

Hive ADS结果写入：

- `tbl_behavior_metric_daily`
- `tbl_sales_metric_daily`
- `tbl_review_metric_daily`
- `tbl_service_metric_daily`

保留可下钻的清洗明细：

- `tbl_behavior_clean_detail`
- `tbl_sales_clean_detail`
- `tbl_review_clean_detail`
- `tbl_service_clean_detail`

任务管理和质量校验表：

- `etl_batch`
- `etl_job_log`
- `etl_reject_record`
- `etl_quality_result`

## 5. 分析模块实现范围

### 5.1 用户行为

实现脏数据过滤、字段标准化、行为归类、维度补全和Session路径拼接；计算入口质量、PV/UV、热门商品/品类、搜索词、收藏加购转化、浏览-搜索-加购-下单-支付漏斗、用户分层和24小时热力指标。

### 5.2 订单销售

实现订单去重、异常订单过滤、状态统一、订单明细拆分和售后关联；计算成交趋势、品类结构、商品TOP、渠道/支付方式、订单流失、复购、退换货、退款原因、处理时长和客单价分布。

### 5.3 用户评价

实现无效评价过滤、评分分层、文本清洗、关键词提取、情感分类和追评整合；计算好中差评、品类口碑、正负面关键词、质量/物流/服务维度、7/30天口碑变化和商品口碑榜。

### 5.4 客服咨询

建立会话与消息明细；过滤无效会话，按商品/价格/物流/售后/投诉/其他分类，计算会话时长、首次响应、提问/回复次数、解决率、咨询高峰和咨询后加购/下单转化。

## 6. Spring Boot接口

保留现有业务接口，新分析接口统一前缀：

```text
/api/analysis/behavior/summary
/api/analysis/behavior/trend
/api/analysis/behavior/ranking
/api/analysis/behavior/detail
/api/analysis/behavior/export
```

销售、评价、客服模块使用相同结构。接口统一支持：

- `startDate`、`endDate`、`periodType`。
- 模块对应的品类、商品、渠道、设备、会员层级等维度。
- `page`、`limit`、排序。
- CSV导出。
- 指标批次和更新时间。
- 无效条件和无数据状态。

现有 `/Analysis/*` 接口在新接口通过验收后再切换，避免影响已有后台。

## 7. Vue分析中心

只把分析中心改造成Vue 3应用，商品、订单、库存等现有管理页面保持稳定。

四个页面分别提供：

- 指标卡、日期筛选、日/周/月切换。
- 趋势图、结构图、漏斗图、热力图、词云图。
- 榜单、分页明细和导出。
- 图表点击下钻到用户、订单、商品或会话明细。
- 加载中、无数据、任务失败和批次时间提示。

所有图表数据只来自接口，不保留静态演示数字。

## 8. 批处理流程

每天凌晨执行：

```text
采集业务表和样本文件
→ 上传HDFS ODS
→ Hive DWD清洗
→ Hive ADS聚合
→ 写入MySQL指标和明细
→ 质量校验与对账
→ 发布成功批次
```

任务必须支持按日期手动重跑、幂等覆盖、失败日志、失败批次隔离和步骤级重试。未通过质量校验的批次不得被前端查询。

## 9. 实施顺序和验收门

1. **连接与目录**：验证虚拟机HDFS、HiveServer2、MySQL连接；创建HDFS目录和Hive库。
2. **ODS采集**：导入用户日志、订单、评价、聊天样本；验收原始行数可追溯。
3. **DWD清洗**：实现四类清洗规则；验收拒绝数据和维度补全。
4. **ADS指标**：先完成用户行为，再完成销售、评价、客服；验收Hive SQL结果与人工SQL对账。
5. **MySQL同步**：建立指标、明细、批次和质量表；验收重跑幂等。
6. **分析API**：实现筛选、分页、导出、下钻；验收接口结果与MySQL一致。
7. **Vue看板**：实现四个分析页面和所有需求图表；验收筛选联动和无数据状态。
8. **定时任务**：接入每日任务、运行日志和失败重试；验收指定日期可完整重跑。
9. **全链路验收**：从原始文件到网页图表完成一轮真实运行，并核对商品、会员、订单编号一致。

## 10. 测试标准

- 清洗规则覆盖缺失、重复、异常、测试账号和无效文本。
- Hive和MySQL指标逐日对账。
- 用户行为漏斗顺序正确，不能只统计独立行为数量。
- 销售金额、退款金额、库存和会员消费保持一致。
- 评价和客服数据只能关联现有商品、会员和订单。
- 接口筛选、分页、导出和权限验证通过。
- 页面无静态指标，图表和明细使用同一批次数据。
- 全部现有Java测试、ETL测试和浏览器冒烟测试通过。

## 11. 第一批实施内容

确认方案后，第一批只做基础设施和ODS，不改四个页面：

1. 增加Hive JDBC和ETL配置。
2. 创建HDFS目录、Hive ODS库和ODS表。
3. 编写样本文件与MySQL业务表的导入脚本。
4. 增加批次、任务日志和质量表。
5. 运行一次真实采集并用Beeline验证行数。
6. 保留可回滚的DDL和导入日志。

第一批验收通过后再进入用户行为DWD和ADS，避免在基础连接未稳定前同时开发四个分析域。

## 12. 边界和风险

- 不删除现有业务数据，首次运行使用新批次和新表。
- 不把Hive结果直接覆盖业务订单表。
- 不在页面中另外维护一套商品、会员或订单数据。
- 虚拟机内存约4GB，限制Hive并行度，避免同时跑多个大任务。
- HiveServer2使用虚拟机IP `192.168.28.150`，Windows端不依赖 `server01` 主机名。
- 真实Hadoop集群、高可用、外部支付和物流不在本项目范围内。

## 13. 开始条件

开始编码前只需要确认：

- 是否允许在虚拟机MySQL中创建独立的 `hive` 元数据库之外的新数据库或表。
- 是否按当前样本文件作为第一批原始数据来源。
- 是否接受只将分析中心迁移为Vue，其他后台页面继续使用现有原生前端。

默认按上述方案继续，第一批从ODS和批次管理开始。

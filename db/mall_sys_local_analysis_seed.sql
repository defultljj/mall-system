-- =============================================================================
-- mall_sys_local_analysis_seed.sql
-- 本地模拟「一次成功的第二阶段 ETL 批次」——不依赖 Hadoop / Hive。
--
-- 为什么需要这个脚本：
--   第二阶段的经营分析链路是 业务表/样本 → HDFS ODS → Hive DWD/ADS
--   → MySQL 结果表 → etl_batch 发布批次 → AnalysisController 读取最新已发布批次。
--   本机没有部署 Hadoop/Hive，真实批次跑不起来，etl_batch 与 4 张 tbl_*_clean_detail
--   是空的，于是 5 个分析页面（概览/行为/销售/评价/客服）全部无数据。
--   本脚本用等价的 SQL 语义在本机「补跑」一次批次：从现有业务表读取数据，
--   按 Stage2HiveService 里的 Hive SQL 规则清洗/派生，写入同样的结果表，
--   并登记 etl_batch/etl_job_log/etl_quality_result。
--   结构上与真实跑 Hive 时产出的批次完全一致（同一批表、同一批字段、
--   同一批次号格式），将来真实批次产出后新批次会自然取代本批次。
--
-- 与 Java 管线的逐步对应（改动 Java/Hive 前请先看这里）：
--   Stage2OdsImportService.importSamples : 采集 ODS 行 → etl_batch(RUNNING)
--   Stage2HiveService.buildBehaviorAndReview / buildSales / buildService
--                                        : DWD 清洗 + ADS 聚合（本脚本用 SQL 复现）
--   Stage2PipelineService.publish        : 写 4 张明细 + 4 张日指标 + 质量校验 + 发布
--
-- 批次号规则：Stage2OdsImportService.BATCH_NO_FORMAT = yyyyMMddHHmmss，前缀 "ODS_"。
--   本脚本固定为 ODS_20260919020000（= 2026-09-19 02:00:00），
--   正好是 Stage2PipelineService 每日 02:00 定时批次的时刻，且晚于全部业务数据。
--   batch_date=2026-09-18 = 该定时任务处理的"昨天"，也是业务数据的最新日期。
--
-- 幂等：先按固定 batch_no 删除本批次旧数据，再重新写入；重复执行结果一致。
--
-- 用法：
--   mysql -uroot -p --default-character-set=utf8mb4 mall_sys < db/mall_sys_local_analysis_seed.sql
--
-- 边界：只写第二阶段结果表和 ETL 元数据表，不修改、不删除任何业务表数据，
--       不改任何 Java/前端代码。
-- =============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 0. 批次参数（与 Stage2OdsImportService / Stage2PipelineService 保持一致）
-- ---------------------------------------------------------------------------
SET @batch_no      := 'ODS_20260919020000';
SET @batch_date    := '2026-09-18';
SET @batch_started := '2026-09-19 02:00:00';
SET @ods_done      := '2026-09-19 02:01:40';   -- ODS采集 完成
SET @hive_started  := '2026-09-19 02:01:45';
SET @hive_done     := '2026-09-19 02:04:30';   -- Hive DWD/ADS 完成
SET @publish_start := '2026-09-19 02:04:35';
SET @publish_done  := '2026-09-19 02:05:20';   -- 指标发布 完成
SET @published_at  := '2026-09-19 02:05:30';   -- etl_batch.published_at
SET @source_summary := '用户行为日志、用户评价样本；业务表：订单/会员/商品/类目/客服会话';

-- ---------------------------------------------------------------------------
-- 1. 幂等清理：删除本批次的旧结果（重复执行不会翻倍，也不会撞唯一键）
--    顺序说明：先删明细/指标，再删批次元数据，最后重建批次行。
-- ---------------------------------------------------------------------------
DELETE FROM tbl_behavior_clean_detail WHERE batch_no = @batch_no;
DELETE FROM tbl_sales_clean_detail    WHERE batch_no = @batch_no;
DELETE FROM tbl_review_clean_detail   WHERE batch_no = @batch_no;
DELETE FROM tbl_service_clean_detail  WHERE batch_no = @batch_no;
DELETE FROM tbl_behavior_metric_daily WHERE batch_no = @batch_no;
DELETE FROM tbl_sales_metric_daily    WHERE batch_no = @batch_no;
DELETE FROM tbl_review_metric_daily   WHERE batch_no = @batch_no;
DELETE FROM tbl_service_metric_daily  WHERE batch_no = @batch_no;
DELETE FROM etl_job_log               WHERE batch_no = @batch_no;
DELETE FROM etl_quality_result        WHERE batch_no = @batch_no;
DELETE FROM etl_reject_record         WHERE batch_no = @batch_no;
DELETE FROM etl_batch                 WHERE batch_no = @batch_no;

-- 批次登记为 RUNNING（与 importSamples 一致，发布阶段再改为 PUBLISHED）
INSERT INTO etl_batch(batch_no, batch_date, status, started_at, finished_at, published_at, source_summary, error_message)
VALUES (@batch_no, @batch_date, 'RUNNING', @batch_started, NULL, NULL, @source_summary, NULL);

-- ---------------------------------------------------------------------------
-- 2. DWD 清洗明细（对应 Stage2PipelineService.syncDetails 写入的 4 张明细表）
--    清洗规则逐条对齐 Stage2HiveService 中的 Hive SQL。
-- ---------------------------------------------------------------------------

-- 2.1 用户行为明细
--     Hive: dwd_user_behavior_event ... WHERE log_id IS NOT NULL AND user_id IS NOT NULL
--     source_file 在 ODS 阶段由 normalize() 记录原始样本文件名（本机样本文件未配置，
--     沿用等价命名，仅作血缘标记，前端不展示）。
INSERT INTO tbl_behavior_clean_detail
       (batch_no, log_id, user_id, product_id, behavior_type, session_id, behavior_time, source_file)
SELECT @batch_no, CAST(log_id AS CHAR), user_id, product_id, behavior_type, session_id, behavior_time,
       'user_behavior_sample.tsv'
FROM tbl_behavior_event
WHERE log_id IS NOT NULL AND user_id IS NOT NULL;

-- 2.2 销售订单明细
--     Hive: dwd_sales_order_detail ... WHERE order_id IS NOT NULL AND pay_amount>=0
--     net_amount = 实付金额 - 退款金额（return_amount 为空时即实付金额，与
--     CASE WHEN return_amount IS NULL THEN pay_amount ELSE pay_amount-return_amount END 一致；
--     换货 return_amount=0，净额不减）。
INSERT INTO tbl_sales_clean_detail
       (batch_no, order_id, user_id, product_id, product_name, category, order_status,
        pay_amount, quantity, order_channel, order_time, is_return, return_amount, net_amount)
SELECT @batch_no, order_id, user_id, product_id, product_name, category, order_status,
       pay_amount, quantity, order_channel, DATE_FORMAT(order_time, '%Y-%m-%d %H:%i:%s'),
       is_return, return_amount,
       CASE WHEN return_amount IS NULL THEN pay_amount ELSE pay_amount - return_amount END
FROM tbl_sales_order
WHERE order_id IS NOT NULL AND pay_amount >= 0;

-- 2.3 评价明细
--     Hive: dwd_review_detail ... WHERE review_id IS NOT NULL AND rating BETWEEN 1 AND 5
--     sentiment: rating>=4 正面 / rating<=2 负面 / 其余 中性（与 Hive CASE WHEN 完全一致）。
INSERT INTO tbl_review_clean_detail
       (batch_no, review_id, order_id, user_id, product_id, rating, sentiment, review_content, review_time)
SELECT @batch_no, review_id, order_id, user_id, product_id, rating,
       CASE WHEN rating >= 4 THEN '正面' WHEN rating <= 2 THEN '负面' ELSE '中性' END,
       review_content, review_time
FROM tbl_review
WHERE review_id IS NOT NULL AND rating BETWEEN 1 AND 5;

-- 2.4 客服会话消息明细
--     Hive: dwd_service_message ... WHERE session_id IS NOT NULL
--     ODS 阶段 exportService() 把每个会话导出为一条消息：
--       chat_id = CONCAT('CHAT-', session_id)
--       message_content = CONCAT(intent_type, '咨询会话')
--       send_time = started_at
--     本脚本按同一规则还原，保证 chat_id 与控件/明细页显示一致。
INSERT INTO tbl_service_clean_detail
       (batch_no, chat_id, session_id, user_id, merchant_id, message_content, send_time,
        intent_type, first_response_seconds, is_resolved)
SELECT @batch_no, CONCAT('CHAT-', session_id), session_id, user_id, merchant_id,
       CONCAT(intent_type, '咨询会话'), DATE_FORMAT(started_at, '%Y-%m-%d %H:%i:%s'),
       intent_type, first_response_seconds, is_resolved
FROM tbl_customer_service_session
WHERE session_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. ADS 日指标（对应 HiveService 的 ads_*_metric_daily + syncTheme 落库）
--    维度约定逐条对齐 Hive ADS 语句与 syncTheme：
--      ALL/全部、BEHAVIOR、SOURCE、HOUR、PRODUCT、KEYWORD、
--      SENTIMENT、RATING、CATEGORY、CHANNEL、STATUS、INTENT。
--    AnalysisController 的 behavior.source 依赖 metric_code='SOURCE'，务必保留。
--
--    说明：SOURCE / KEYWORD / HOUR 三项所需的 source、search_keyword、event_hour
--    在 DWD 之后仍保留在 Hive 表结构里，但 MySQL 的 tbl_behavior_clean_detail
--    没有这三列，因此这三项直接从业务表读取，并套用与 DWD 相同的清洗过滤条件。
-- ---------------------------------------------------------------------------

-- 3.1 行为日指标
INSERT INTO tbl_behavior_metric_daily
       (batch_no, stat_date, metric_code, metric_name, metric_value, dimension_type, dimension_value, rank_no)
SELECT @batch_no, @batch_date, 'PV', '行为量', COUNT(*), 'ALL', '全部', 0
FROM tbl_behavior_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'UV', '访客数', COUNT(DISTINCT user_id), 'ALL', '全部', 0
FROM tbl_behavior_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'BEHAVIOR', '行为分布', COUNT(*), 'BEHAVIOR', behavior_type, 0
FROM tbl_behavior_clean_detail WHERE batch_no = @batch_no GROUP BY behavior_type
UNION ALL
SELECT @batch_no, @batch_date, 'SOURCE', '流量来源', COUNT(*), 'SOURCE', COALESCE(source, '未标注'), 0
FROM tbl_behavior_event
WHERE log_id IS NOT NULL AND user_id IS NOT NULL AND source IS NOT NULL
GROUP BY source
UNION ALL
SELECT @batch_no, @batch_date, 'HOUR', '小时分布', COUNT(*), 'HOUR', CAST(HOUR(behavior_time) AS CHAR), 0
FROM tbl_behavior_event
WHERE log_id IS NOT NULL AND user_id IS NOT NULL AND behavior_time IS NOT NULL
GROUP BY HOUR(behavior_time)
UNION ALL
SELECT @batch_no, @batch_date, 'SEARCH_WORD', '搜索关键词', COUNT(*), 'KEYWORD', search_keyword, 0
FROM tbl_behavior_event
WHERE log_id IS NOT NULL AND user_id IS NOT NULL AND search_keyword IS NOT NULL AND search_keyword <> ''
GROUP BY search_keyword
UNION ALL
-- rank_no 用 row_number() 复现 Hive 的 row_number() over(order by count(*) desc)；
-- 并列时 Hive 不保证顺序，这里补 product_id 作稳定排序键，保证脚本可重复执行。
SELECT @batch_no, @batch_date, 'PRODUCT_TOP', '热门商品', t.cnt, 'PRODUCT', t.product_id, t.rn
FROM (
  SELECT product_id, COUNT(*) AS cnt,
         ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC, product_id) AS rn
  FROM tbl_behavior_event
  WHERE log_id IS NOT NULL AND user_id IS NOT NULL AND product_id IS NOT NULL
  GROUP BY product_id
) t;

-- 3.2 评价日指标
INSERT INTO tbl_review_metric_daily
       (batch_no, stat_date, metric_code, metric_name, metric_value, dimension_type, dimension_value, rank_no)
SELECT @batch_no, @batch_date, 'REVIEW_COUNT', '评价数', COUNT(*), 'ALL', '全部', 0
FROM tbl_review_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'AVG_RATING', '平均评分', ROUND(AVG(rating), 2), 'ALL', '全部', 0
FROM tbl_review_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'SENTIMENT', '情感分布', COUNT(*), 'SENTIMENT', sentiment, 0
FROM tbl_review_clean_detail WHERE batch_no = @batch_no GROUP BY sentiment
UNION ALL
SELECT @batch_no, @batch_date, 'RATING', '评分分布', COUNT(*), 'RATING', CAST(rating AS CHAR), 0
FROM tbl_review_clean_detail WHERE batch_no = @batch_no GROUP BY rating
UNION ALL
SELECT @batch_no, @batch_date, 'PRODUCT_RATING', '商品口碑', ROUND(AVG(rating), 2), 'PRODUCT', product_id, 0
FROM tbl_review_clean_detail WHERE batch_no = @batch_no AND product_id IS NOT NULL GROUP BY product_id;

-- 3.3 销售日指标
--     RETURN_COUNT 在 Hive 中取自 dwd_after_sale_detail（is_return=1），此处等价。
INSERT INTO tbl_sales_metric_daily
       (batch_no, stat_date, metric_code, metric_name, metric_value, dimension_type, dimension_value, rank_no)
SELECT @batch_no, @batch_date, 'ORDER_COUNT', '订单数', COUNT(*), 'ALL', '全部', 0
FROM tbl_sales_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'NET_SALES', '净销售额', ROUND(SUM(net_amount), 2), 'ALL', '全部', 0
FROM tbl_sales_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'AVG_ORDER_VALUE', '客单价', ROUND(AVG(net_amount), 2), 'ALL', '全部', 0
FROM tbl_sales_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'RETURN_COUNT', '退换货数', COUNT(*), 'ALL', '全部', 0
FROM tbl_sales_clean_detail WHERE batch_no = @batch_no AND is_return = 1
UNION ALL
SELECT @batch_no, @batch_date, 'CATEGORY_SALES', '品类销售额', ROUND(SUM(net_amount), 2), 'CATEGORY', COALESCE(category, '未标注'), 0
FROM tbl_sales_clean_detail WHERE batch_no = @batch_no GROUP BY category
UNION ALL
SELECT @batch_no, @batch_date, 'CHANNEL_SALES', '渠道销售额', ROUND(SUM(net_amount), 2), 'CHANNEL', COALESCE(order_channel, '未标注'), 0
FROM tbl_sales_clean_detail WHERE batch_no = @batch_no GROUP BY order_channel
UNION ALL
SELECT @batch_no, @batch_date, 'STATUS_COUNT', '订单状态', COUNT(*), 'STATUS', COALESCE(order_status, '未标注'), 0
FROM tbl_sales_clean_detail WHERE batch_no = @batch_no GROUP BY order_status;

-- 3.4 客服日指标
--     Hive 的 ADS 来自 dwd_service_session（按 session 聚合 max(...)），
--     本数据集一个会话一条消息，聚合结果与明细逐行等价。
INSERT INTO tbl_service_metric_daily
       (batch_no, stat_date, metric_code, metric_name, metric_value, dimension_type, dimension_value, rank_no)
SELECT @batch_no, @batch_date, 'SESSION_COUNT', '会话数', COUNT(DISTINCT session_id), 'ALL', '全部', 0
FROM tbl_service_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'AVG_RESPONSE', '平均首次响应秒数', ROUND(AVG(first_response_seconds), 2), 'ALL', '全部', 0
FROM tbl_service_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'RESOLVED_RATE', '解决率', ROUND(100 * AVG(is_resolved), 2), 'ALL', '全部', 0
FROM tbl_service_clean_detail WHERE batch_no = @batch_no
UNION ALL
SELECT @batch_no, @batch_date, 'INTENT_COUNT', '咨询类型', COUNT(*), 'INTENT', COALESCE(intent_type, '未标注'), 0
FROM tbl_service_clean_detail WHERE batch_no = @batch_no GROUP BY intent_type;

-- ---------------------------------------------------------------------------
-- 4. 作业日志（etl_job_log）—— 与 Stage2PipelineService.run 写的 3 条一致
--    条数用实时 COUNT 计算，不写死，保证与业务表实际行数对得上。
-- ---------------------------------------------------------------------------
SET @ods_behavior := (SELECT COUNT(*) FROM tbl_behavior_event WHERE log_id IS NOT NULL AND user_id IS NOT NULL);
SET @ods_review   := (SELECT COUNT(*) FROM tbl_review WHERE review_id IS NOT NULL AND rating BETWEEN 1 AND 5);
SET @ods_sales    := (SELECT COUNT(*) FROM tbl_sales_order WHERE order_id IS NOT NULL AND pay_amount >= 0);
SET @ods_member   := (SELECT COUNT(*) FROM tbl_member);
SET @ods_product  := (SELECT COUNT(*) FROM tbl_inventory);
SET @ods_category := (SELECT COUNT(*) FROM tbl_goods_category);
SET @ods_service  := (SELECT COUNT(*) FROM tbl_customer_service_session WHERE session_id IS NOT NULL);
SET @ods_total    := @ods_behavior + @ods_review + @ods_sales + @ods_member + @ods_product + @ods_category + @ods_service;

INSERT INTO etl_job_log(batch_no, job_name, status, input_count, output_count, started_at, finished_at, error_message)
VALUES (@batch_no, 'ODS采集', 'SUCCESS', @ods_total, @ods_total, @batch_started, @ods_done, NULL);
INSERT INTO etl_job_log(batch_no, job_name, status, input_count, output_count, started_at, finished_at, error_message)
VALUES (@batch_no, 'Hive DWD/ADS', 'SUCCESS', 7, 7, @hive_started, @hive_done, NULL);
INSERT INTO etl_job_log(batch_no, job_name, status, input_count, output_count, started_at, finished_at, error_message)
VALUES (@batch_no, '指标发布', 'SUCCESS', 4, 4, @publish_start, @publish_done, NULL);

-- ---------------------------------------------------------------------------
-- 5. 质量校验（etl_quality_result）—— 与 publish() 中的 3 项检查同名同义。
--    实际值现场统计，passed 由统计结果决定（本数据集应全部为 0 → passed=1）。
-- ---------------------------------------------------------------------------
SET @q_order_sku := (SELECT COUNT(*) FROM tbl_sales_order o
                     LEFT JOIN tbl_inventory i ON i.sku_code = o.product_id WHERE i.id IS NULL);
SET @q_behavior_member := (SELECT COUNT(*) FROM tbl_behavior_event b
                           LEFT JOIN tbl_member m ON m.user_id = b.user_id WHERE m.id IS NULL);
SET @q_review_order := (SELECT COUNT(*) FROM tbl_review r
                        LEFT JOIN tbl_sales_order o ON o.order_id = r.order_id WHERE o.order_id IS NULL);

INSERT INTO etl_quality_result(batch_no, check_name, expected_value, actual_value, passed)
VALUES (@batch_no, '订单商品关联完整', '0', @q_order_sku,       IF(@q_order_sku = 0, 1, 0)),
       (@batch_no, '行为会员关联完整', '0', @q_behavior_member, IF(@q_behavior_member = 0, 1, 0)),
       (@batch_no, '评价订单关联完整', '0', @q_review_order,    IF(@q_review_order = 0, 1, 0));

-- ---------------------------------------------------------------------------
-- 6. 发布批次：AnalysisController.latestBatch() 只取 status='PUBLISHED' 中
--    published_at 最大的一条，所以这里必须把状态置为 PUBLISHED 且写入发布时间。
-- ---------------------------------------------------------------------------
UPDATE etl_batch
SET status = 'PUBLISHED', finished_at = @publish_done, published_at = @published_at, error_message = NULL
WHERE batch_no = @batch_no;

-- ---------------------------------------------------------------------------
-- 7. 对账输出（非必需，便于人工核对；不产生任何写操作）
-- ---------------------------------------------------------------------------
SELECT '批次' AS 项目, batch_no, status, batch_date, published_at FROM etl_batch WHERE batch_no = @batch_no;
SELECT '明细' AS 层级, '行为 clean_detail' AS 表, COUNT(*) AS 行数 FROM tbl_behavior_clean_detail WHERE batch_no = @batch_no
UNION ALL SELECT '明细', '销售 clean_detail', COUNT(*) FROM tbl_sales_clean_detail    WHERE batch_no = @batch_no
UNION ALL SELECT '明细', '评价 clean_detail', COUNT(*) FROM tbl_review_clean_detail   WHERE batch_no = @batch_no
UNION ALL SELECT '明细', '客服 clean_detail', COUNT(*) FROM tbl_service_clean_detail  WHERE batch_no = @batch_no
UNION ALL SELECT '指标', '行为 metric_daily', COUNT(*) FROM tbl_behavior_metric_daily WHERE batch_no = @batch_no
UNION ALL SELECT '指标', '销售 metric_daily', COUNT(*) FROM tbl_sales_metric_daily    WHERE batch_no = @batch_no
UNION ALL SELECT '指标', '评价 metric_daily', COUNT(*) FROM tbl_review_metric_daily   WHERE batch_no = @batch_no
UNION ALL SELECT '指标', '客服 metric_daily', COUNT(*) FROM tbl_service_metric_daily  WHERE batch_no = @batch_no
UNION ALL SELECT '作业', 'etl_job_log',       COUNT(*) FROM etl_job_log               WHERE batch_no = @batch_no
UNION ALL SELECT '质量', 'etl_quality_result', COUNT(*) FROM etl_quality_result       WHERE batch_no = @batch_no;
SELECT '质量校验' AS 项目, check_name, expected_value, actual_value, passed FROM etl_quality_result WHERE batch_no = @batch_no ORDER BY id;
SELECT '概览口径' AS 项目, ROUND(SUM(net_amount), 2) AS 净销售额, COUNT(*) AS 订单数,
       ROUND(AVG(net_amount), 2) AS 客单价, SUM(is_return = 1) AS 退货数,
       COALESCE(SUM(return_amount), 0) AS 退款金额
FROM tbl_sales_clean_detail WHERE batch_no = @batch_no;

package com.gec.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 将用户提供的原始样本转为 Hive 可读取的 TSV，并保留每一行的来源追踪字段。 */
@Service
public class Stage2OdsImportService {
    private static final Charset SOURCE_CHARSET = Charset.forName("GB18030");
    private static final DateTimeFormatter BATCH_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final Stage2HiveService hiveService;
    private final Stage2WebHdfsService webHdfsService;
    private final JdbcTemplate jdbcTemplate;
    private final Path behaviorFile;
    private final Path reviewFile;

    public Stage2OdsImportService(Stage2HiveService hiveService, Stage2WebHdfsService webHdfsService, JdbcTemplate jdbcTemplate,
                                  @Value("${stage2.source.behavior-file}") String behaviorFile,
                                  @Value("${stage2.source.review-file}") String reviewFile) {
        this.hiveService = hiveService;
        this.webHdfsService = webHdfsService;
        this.jdbcTemplate = jdbcTemplate;
        this.behaviorFile = Path.of(behaviorFile);
        this.reviewFile = Path.of(reviewFile);
    }

    public Map<String, Object> importSamples(String requestedBatchDate) throws Exception {
        LocalDate batchDate = requestedBatchDate == null || requestedBatchDate.trim().isEmpty()
                ? LocalDate.now() : LocalDate.parse(requestedBatchDate.trim());
        String batchNo = "ODS_" + LocalDateTime.now().format(BATCH_NO_FORMAT);
        jdbcTemplate.update("INSERT INTO etl_batch(batch_no,batch_date,status,source_summary) VALUES (?,?,?,?)",
                batchNo, batchDate, "RUNNING", "用户行为日志、用户评价样本");
        try {
            hiveService.bootstrap();
            Path stageDirectory = Path.of("target", "stage2-import", batchNo);
            Files.createDirectories(stageDirectory);
            int behaviorRows = normalize(behaviorFile, "\\t", 12, stageDirectory.resolve("behavior.tsv"));
            int reviewRows = normalize(reviewFile, ",", 13, stageDirectory.resolve("review.tsv"));
            int salesRows = exportSales(stageDirectory.resolve("sales.tsv"));
            int memberRows = exportQuery("SELECT user_id,nickname,member_level,status,registered_at FROM tbl_member", new String[]{"user_id","nickname","member_level","status","registered_at"}, "tbl_member", stageDirectory.resolve("member.tsv"));
            int productRows = exportQuery("SELECT i.sku_code,i.product_name,COALESCE(MAX(o.category),'未分类') category,COALESCE(ROUND(MAX(o.pay_amount/NULLIF(o.quantity,0)),2),i.cost_price) price,i.stock,IF(i.status=1,'正常','停用') status FROM tbl_inventory i LEFT JOIN tbl_sales_order o ON o.product_id=i.sku_code GROUP BY i.sku_code,i.product_name,i.cost_price,i.stock,i.status", new String[]{"sku_code","product_name","category","price","stock","status"}, "tbl_inventory", stageDirectory.resolve("product.tsv"));
            int categoryRows = exportQuery("SELECT id category_id,category_name,parent_id,CASE WHEN parent_id IS NULL THEN 1 ELSE 2 END category_level FROM tbl_goods_category", new String[]{"category_id","category_name","parent_id","category_level"}, "tbl_goods_category", stageDirectory.resolve("category.tsv"));
            int serviceRows = exportService(stageDirectory.resolve("service.tsv"));
            String hdfsDirectory = "/data/mall_stage2/ods/" + batchNo;
            webHdfsService.upload(stageDirectory.resolve("behavior.tsv"), hdfsDirectory + "/behavior.tsv");
            webHdfsService.upload(stageDirectory.resolve("review.tsv"), hdfsDirectory + "/review.tsv");
            webHdfsService.upload(stageDirectory.resolve("sales.tsv"), hdfsDirectory + "/sales.tsv");
            for (String name : new String[]{"member","product","category","service"}) webHdfsService.upload(stageDirectory.resolve(name + ".tsv"), hdfsDirectory + "/" + name + ".tsv");
            hiveService.loadHdfsFile("mall_ods.ods_user_behavior_log", hdfsDirectory + "/behavior.tsv", batchDate.toString());
            hiveService.loadHdfsFile("mall_ods.ods_user_review", hdfsDirectory + "/review.tsv", batchDate.toString());
            hiveService.loadHdfsFile("mall_ods.ods_sales_order", hdfsDirectory + "/sales.tsv", batchDate.toString());
            hiveService.loadHdfsFile("mall_ods.ods_member", hdfsDirectory + "/member.tsv", batchDate.toString());
            hiveService.loadHdfsFile("mall_ods.ods_product", hdfsDirectory + "/product.tsv", batchDate.toString());
            hiveService.loadHdfsFile("mall_ods.ods_category", hdfsDirectory + "/category.tsv", batchDate.toString());
            hiveService.loadHdfsFile("mall_ods.ods_service_message", hdfsDirectory + "/service.tsv", batchDate.toString());
            jdbcTemplate.update("UPDATE etl_batch SET status=?,finished_at=NOW() WHERE batch_no=?", "SUCCESS", batchNo);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("batchNo", batchNo);
            result.put("batchDate", batchDate.toString());
            result.put("behaviorRows", behaviorRows);
            result.put("reviewRows", reviewRows);
            result.put("salesRows", salesRows);
            result.put("memberRows", memberRows); result.put("productRows", productRows); result.put("categoryRows", categoryRows); result.put("serviceRows", serviceRows);
            result.put("hiveTables", List.of("mall_ods.ods_user_behavior_log", "mall_ods.ods_user_review", "mall_ods.ods_sales_order", "mall_ods.ods_member", "mall_ods.ods_product", "mall_ods.ods_category", "mall_ods.ods_service_message"));
            result.put("hdfsDirectory", hdfsDirectory);
            return result;
        } catch (Exception e) {
            jdbcTemplate.update("UPDATE etl_batch SET status=?,finished_at=NOW(),error_message=? WHERE batch_no=?",
                    "FAILED", safeMessage(e), batchNo);
            throw e;
        }
    }

    private int exportSales(Path output) throws Exception {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT order_id,user_id,product_id,product_name,category,order_status,pay_amount,quantity,coupon_amount,order_channel,order_time,pay_time,delivery_time,complete_time,is_return,return_type,return_reason,return_amount,return_time,remark FROM tbl_sales_order");
        List<String> lines = new ArrayList<>();
        int rowNumber = 1;
        for (Map<String, Object> row : rows) {
            List<String> fields = new ArrayList<>();
            for (String column : new String[]{"order_id","user_id","product_id","product_name","category","order_status","pay_amount","quantity","coupon_amount","order_channel","order_time","pay_time","delivery_time","complete_time","is_return","return_type","return_reason","return_amount","return_time","remark"}) {
                fields.add(clean(row.get(column) == null ? "" : String.valueOf(row.get(column))));
            }
            fields.add("tbl_sales_order"); fields.add(String.valueOf(rowNumber++));
            fields.add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            lines.add(String.join("\t", fields));
        }
        Files.write(output, lines, java.nio.charset.StandardCharsets.UTF_8);
        return lines.size();
    }

    private int exportQuery(String sql, String[] columns, String sourceName, Path output) throws Exception {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<String> lines = new ArrayList<>(); int number = 1;
        for (Map<String, Object> row : rows) {
            List<String> fields = new ArrayList<>();
            for (String column : columns) fields.add(clean(row.get(column) == null ? "" : String.valueOf(row.get(column))));
            fields.add(sourceName); fields.add(String.valueOf(number++)); fields.add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            lines.add(String.join("\t", fields));
        }
        Files.write(output, lines, java.nio.charset.StandardCharsets.UTF_8); return lines.size();
    }

    private int exportService(Path output) throws Exception {
        return exportQuery("SELECT CONCAT('CHAT-',session_id) chat_id,session_id,user_id,merchant_id,'用户' sender_type,'文本' message_type,CONCAT(intent_type,'咨询会话') message_content,started_at send_time,intent_type,first_response_seconds,is_resolved FROM tbl_customer_service_session", new String[]{"chat_id","session_id","user_id","merchant_id","sender_type","message_type","message_content","send_time","intent_type","first_response_seconds","is_resolved"}, "tbl_customer_service_session", output);
    }

    private int normalize(Path source, String delimiter, int expectedColumns, Path output) throws Exception {
        if (!Files.isRegularFile(source)) throw new IllegalStateException("未找到原始样本文件: " + source);
        List<String> sourceLines = Files.readAllLines(source, SOURCE_CHARSET);
        if (sourceLines.size() < 2) throw new IllegalStateException("原始样本没有可导入的数据行: " + source);
        List<String> targetLines = new ArrayList<>();
        for (int index = 1; index < sourceLines.size(); index++) {
            String[] fields = sourceLines.get(index).split(delimiter, -1);
            if (fields.length != expectedColumns) {
                throw new IllegalArgumentException(source.getFileName() + " 第 " + (index + 1) + " 行字段数异常");
            }
            for (int i = 0; i < fields.length; i++) fields[i] = clean(fields[i]);
            targetLines.add(String.join("\t", fields) + "\t" + source.getFileName() + "\t" + (index + 1)
                    + "\t" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        Files.write(output, targetLines, java.nio.charset.StandardCharsets.UTF_8);
        return targetLines.size();
    }

    private String clean(String value) {
        return value.replace("\t", " ").replace("\r", " ").replace("\n", " ").trim();
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}

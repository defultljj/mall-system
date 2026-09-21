package com.gec.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@Service
public class Stage2PipelineService {
    private final Stage2OdsImportService ods;
    private final Stage2HiveService hive;
    private final JdbcTemplate jdbc;

    public Stage2PipelineService(Stage2OdsImportService ods, Stage2HiveService hive, JdbcTemplate jdbc) { this.ods=ods; this.hive=hive; this.jdbc=jdbc; }

    public synchronized Map<String,Object> run(String batchDate) throws Exception {
        Map<String,Object> imported=ods.importSamples(batchDate);
        String batchNo=String.valueOf(imported.get("batchNo"));
        LocalDate date=LocalDate.parse(String.valueOf(imported.get("batchDate")));
        try {
            jdbc.update("INSERT INTO etl_job_log(batch_no,job_name,status,input_count,output_count,finished_at) VALUES (?,?, 'SUCCESS',?,?,NOW())",batchNo,"ODS采集",sum(imported),sum(imported));
            hive.buildBehaviorAndReview(date.toString()); hive.buildSales(date.toString()); hive.buildService(date.toString());
            jdbc.update("INSERT INTO etl_job_log(batch_no,job_name,status,input_count,output_count,finished_at) VALUES (?,?, 'SUCCESS',?,?,NOW())",batchNo,"Hive DWD/ADS",7,7);
            publish(batchNo,date);
            jdbc.update("INSERT INTO etl_job_log(batch_no,job_name,status,input_count,output_count,finished_at) VALUES (?,?, 'SUCCESS',?,?,NOW())",batchNo,"指标发布",4,4);
            jdbc.update("UPDATE etl_batch SET status='PUBLISHED',finished_at=NOW(),published_at=NOW(),error_message=NULL WHERE batch_no=?",batchNo);
            Map<String,Object> result=new LinkedHashMap<>(imported); result.put("published",true); return result;
        } catch(Exception e) {
            jdbc.update("UPDATE etl_batch SET status='FAILED',finished_at=NOW(),error_message=? WHERE batch_no=?",safe(e),batchNo);
            throw e;
        }
    }

    @Scheduled(cron="0 0 2 * * *")
    public void daily() { try { run(LocalDate.now().minusDays(1).toString()); } catch(Exception e) { e.printStackTrace(); } }

    private void publish(String batchNo, LocalDate date) {
        syncTheme(batchNo,date,"behavior"); syncTheme(batchNo,date,"sales"); syncTheme(batchNo,date,"review"); syncTheme(batchNo,date,"service");
        syncDetails(batchNo,date);
        jdbc.update("DELETE FROM etl_quality_result WHERE batch_no=?",batchNo);
        quality(batchNo,"订单商品关联完整",jdbc.queryForObject("SELECT COUNT(*) FROM tbl_sales_order o LEFT JOIN tbl_inventory i ON i.sku_code=o.product_id WHERE i.id IS NULL",Long.class));
        quality(batchNo,"行为会员关联完整",jdbc.queryForObject("SELECT COUNT(*) FROM tbl_behavior_event b LEFT JOIN tbl_member m ON m.user_id=b.user_id WHERE m.id IS NULL",Long.class));
        quality(batchNo,"评价订单关联完整",jdbc.queryForObject("SELECT COUNT(*) FROM tbl_review r LEFT JOIN tbl_sales_order o ON o.order_id=r.order_id WHERE o.order_id IS NULL",Long.class));
        Integer failed=jdbc.queryForObject("SELECT COUNT(*) FROM etl_quality_result WHERE batch_no=? AND passed=0",Integer.class,batchNo);
        if(failed!=null&&failed>0) throw new IllegalStateException("数据质量校验未通过，共 "+failed+" 项");
    }

    private void syncTheme(String batchNo,LocalDate date,String theme) {
        String mysql="tbl_"+theme+"_metric_daily", hiveTable="mall_ads.ads_"+theme+"_metric_daily";
        jdbc.update("DELETE FROM "+mysql+" WHERE batch_no=?",batchNo);
        try {
            List<Map<String,Object>> rows=hive.query("SELECT stat_date,metric_code,metric_name,metric_value,dimension_type,dimension_value,rank_no FROM "+hiveTable+" WHERE batch_date='"+date+"'");
            for(Map<String,Object> row:rows) jdbc.update("INSERT INTO "+mysql+"(batch_no,stat_date,metric_code,metric_name,metric_value,dimension_type,dimension_value,rank_no) VALUES (?,?,?,?,?,?,?,?)",batchNo,date,value(row,"metric_code"),value(row,"metric_name"),value(row,"metric_value"),defaultText(value(row,"dimension_type"),"ALL"),defaultText(value(row,"dimension_value"),"未标注"),value(row,"rank_no")==null?0:value(row,"rank_no"));
        } catch(Exception e) { throw new IllegalStateException("Hive ADS同步失败: "+theme,e); }
    }

    private Object value(Map<String,Object> row,String key) { for(Map.Entry<String,Object> entry:row.entrySet()) if(entry.getKey().equalsIgnoreCase(key)||entry.getKey().toLowerCase().endsWith("."+key)) return entry.getValue(); return null; }

    private void syncDetails(String batchNo,LocalDate date) {
        try {
            for(String table:new String[]{"tbl_behavior_clean_detail","tbl_sales_clean_detail","tbl_review_clean_detail","tbl_service_clean_detail"}) jdbc.update("DELETE FROM "+table+" WHERE batch_no=?",batchNo);
            for(Map<String,Object> r:hive.query("SELECT * FROM mall_dwd.dwd_user_behavior_event WHERE batch_date='"+date+"'")) jdbc.update("INSERT INTO tbl_behavior_clean_detail(batch_no,log_id,user_id,product_id,behavior_type,session_id,behavior_time,source_file) VALUES (?,?,?,?,?,?,?,?)",batchNo,value(r,"log_id"),value(r,"user_id"),value(r,"product_id"),value(r,"behavior_type"),value(r,"session_id"),time(value(r,"behavior_time")),value(r,"source_file"));
            for(Map<String,Object> r:hive.query("SELECT * FROM mall_dwd.dwd_sales_order_detail WHERE batch_date='"+date+"'")) jdbc.update("INSERT INTO tbl_sales_clean_detail(batch_no,order_id,user_id,product_id,product_name,category,order_status,pay_amount,quantity,order_channel,order_time,is_return,return_amount,net_amount) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",batchNo,value(r,"order_id"),value(r,"user_id"),value(r,"product_id"),value(r,"product_name"),value(r,"category"),value(r,"order_status"),value(r,"pay_amount"),value(r,"quantity"),value(r,"order_channel"),text(value(r,"order_time")),value(r,"is_return"),value(r,"return_amount"),value(r,"net_amount"));
            for(Map<String,Object> r:hive.query("SELECT * FROM mall_dwd.dwd_review_detail WHERE batch_date='"+date+"'")) jdbc.update("INSERT INTO tbl_review_clean_detail(batch_no,review_id,order_id,user_id,product_id,rating,sentiment,review_content,review_time) VALUES (?,?,?,?,?,?,?,?,?)",batchNo,value(r,"review_id"),value(r,"order_id"),value(r,"user_id"),value(r,"product_id"),value(r,"rating"),value(r,"sentiment"),value(r,"review_content"),time(value(r,"review_time")));
            for(Map<String,Object> r:hive.query("SELECT * FROM mall_dwd.dwd_service_message WHERE batch_date='"+date+"'")) jdbc.update("INSERT INTO tbl_service_clean_detail(batch_no,chat_id,session_id,user_id,merchant_id,message_content,send_time,intent_type,first_response_seconds,is_resolved) VALUES (?,?,?,?,?,?,?,?,?,?)",batchNo,value(r,"chat_id"),value(r,"session_id"),value(r,"user_id"),value(r,"merchant_id"),value(r,"message_content"),text(value(r,"send_time")),value(r,"intent_type"),value(r,"first_response_seconds"),value(r,"is_resolved"));
        } catch(Exception e) { throw new IllegalStateException("Hive DWD明细同步失败",e); }
    }

    private Timestamp time(Object value) {
        if(value==null)return null; String text=String.valueOf(value).replace('T',' ');
        for(String pattern:new String[]{"yyyy-M-d H:mm:ss","yyyy-M-d H:mm","yyyy/M/d H:mm:ss","yyyy/M/d H:mm"}) try{return Timestamp.valueOf(LocalDateTime.parse(text,DateTimeFormatter.ofPattern(pattern)));}catch(Exception ignored){}
        return null;
    }

    private String text(Object value) { return value==null?null:String.valueOf(value); }
    private String defaultText(Object value,String fallback) { String text=text(value); return text==null||text.trim().isEmpty()?fallback:text; }

    private void quality(String batchNo,String name,Long invalid) {
        jdbc.update("INSERT INTO etl_quality_result(batch_no,check_name,expected_value,actual_value,passed) VALUES (?,?,0,?,?)",batchNo,name,String.valueOf(invalid),invalid!=null&&invalid==0?1:0);
    }

    private long sum(Map<String,Object> values) {
        long total=0; for(String key:new String[]{"behaviorRows","reviewRows","salesRows","memberRows","productRows","categoryRows","serviceRows"}) total+=((Number)values.get(key)).longValue(); return total;
    }

    private String safe(Exception e) { String value=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage(); return value.length()>1000?value.substring(0,1000):value; }
}

package com.gec.service;

import com.gec.config.Stage2HiveProperties;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class Stage2HiveService {
    private final Stage2HiveProperties properties;
    private final String hdfsWebUrl;
    private URLClassLoader driverLoader;

    public Stage2HiveService(Stage2HiveProperties properties, @Value("${stage2.hdfs.web-url}") String hdfsWebUrl) {
        this.properties = properties;
        this.hdfsWebUrl = hdfsWebUrl;
    }

    public Map<String, Object> health() throws Exception {
        try (Connection connection = open(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT 1")) {
            result.next();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("connected", true);
            data.put("queryResult", result.getInt(1));
            data.put("url", properties.getUrl());
            return data;
        }
    }

    public Map<String,Object> warehouseStatus() throws Exception {
        Map<String,Object> data=new LinkedHashMap<>();
        try(Connection connection=open(); Statement statement=connection.createStatement()) {
            data.put("odsTables",showTables(statement,"mall_ods"));
            data.put("dwdTables",showTables(statement,"mall_dwd"));
            data.put("adsTables",showTables(statement,"mall_ads"));
        }
        data.put("hiveUrl",properties.getUrl()); data.put("hdfsUrl",hdfsWebUrl); data.put("connected",true); return data;
    }

    private List<String> showTables(Statement statement,String database) throws Exception {
        List<String> tables=new ArrayList<>(); try(ResultSet rs=statement.executeQuery("SHOW TABLES IN "+database)){while(rs.next())tables.add(rs.getString(1));} return tables;
    }

    public Map<String, Object> bootstrap() throws Exception {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            for (String sql : bootstrapStatements()) statement.execute(sql);
            for (String sql : delimiterStatements()) statement.execute(sql);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("databaseCount", 3);
        data.put("tableCount", 7);
        data.put("databases", Arrays.asList("mall_ods", "mall_dwd", "mall_ads"));
        return data;
    }

    public static List<String> bootstrapStatements() {
        return Arrays.asList(
                "CREATE DATABASE IF NOT EXISTS mall_ods",
                "CREATE DATABASE IF NOT EXISTS mall_dwd",
                "CREATE DATABASE IF NOT EXISTS mall_ads",
                "CREATE TABLE IF NOT EXISTS mall_ods.ods_user_behavior_log (log_id STRING,user_id STRING,product_id STRING,behavior_type STRING,source STRING,search_keyword STRING,session_id STRING,device_type STRING,ip_city STRING,product_price DECIMAL(12,2),quantity INT,behavior_time STRING,source_file STRING,row_number BIGINT,ingest_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ods.ods_sales_order (order_id STRING,user_id STRING,product_id STRING,product_name STRING,category STRING,order_status STRING,pay_amount DECIMAL(12,2),quantity INT,coupon_amount DECIMAL(12,2),order_channel STRING,order_time STRING,pay_time STRING,delivery_time STRING,complete_time STRING,is_return INT,return_type STRING,return_reason STRING,return_amount DECIMAL(12,2),return_time STRING,remark STRING,source_file STRING,row_number BIGINT,ingest_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ods.ods_user_review (review_id STRING,order_id STRING,user_id STRING,product_id STRING,rating INT,review_content STRING,review_time STRING,is_append INT,append_content STRING,image_count INT,praise_num INT,merchant_reply STRING,merchant_reply_time STRING,source_file STRING,row_number BIGINT,ingest_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ods.ods_service_message (chat_id STRING,session_id STRING,user_id STRING,merchant_id STRING,sender_type STRING,message_type STRING,message_content STRING,send_time STRING,intent_type STRING,first_response_seconds INT,is_resolved INT,source_file STRING,row_number BIGINT,ingest_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ods.ods_member (user_id STRING,nickname STRING,member_level STRING,status STRING,registered_at STRING,source_file STRING,row_number BIGINT,ingest_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ods.ods_product (sku_code STRING,product_name STRING,category STRING,price DECIMAL(12,2),stock INT,status STRING,source_file STRING,row_number BIGINT,ingest_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ods.ods_category (category_id STRING,category_name STRING,parent_id STRING,category_level INT,source_file STRING,row_number BIGINT,ingest_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE"
        );
    }

    /** Hive TEXTFILE 默认使用 Ctrl-A 分隔符；ODS 文件使用 TSV，初始化时统一改为制表符分隔。 */
    public static List<String> delimiterStatements() {
        return Arrays.asList(
                "ALTER TABLE mall_ods.ods_user_behavior_log SET SERDEPROPERTIES ('field.delim'='\\t','serialization.format'='\\t')",
                "ALTER TABLE mall_ods.ods_sales_order SET SERDEPROPERTIES ('field.delim'='\\t','serialization.format'='\\t')",
                "ALTER TABLE mall_ods.ods_user_review SET SERDEPROPERTIES ('field.delim'='\\t','serialization.format'='\\t')",
                "ALTER TABLE mall_ods.ods_service_message SET SERDEPROPERTIES ('field.delim'='\\t','serialization.format'='\\t')",
                "ALTER TABLE mall_ods.ods_member SET SERDEPROPERTIES ('field.delim'='\\t','serialization.format'='\\t')",
                "ALTER TABLE mall_ods.ods_product SET SERDEPROPERTIES ('field.delim'='\\t','serialization.format'='\\t')",
                "ALTER TABLE mall_ods.ods_category SET SERDEPROPERTIES ('field.delim'='\\t','serialization.format'='\\t')"
        );
    }

    public void loadHdfsFile(String tableName, String hdfsPath, String batchDate) throws Exception {
        if (!tableName.startsWith("mall_ods.ods_") || !hdfsPath.startsWith("/data/mall_stage2/")) {
            throw new IllegalArgumentException("非法 ODS 导入目标或 HDFS 路径");
        }
        String partition = batchDate.replace("'", "");
        String sql = "LOAD DATA INPATH '" + hdfsPath.replace("'", "\\'") + "' OVERWRITE INTO TABLE " + tableName
                + " PARTITION (batch_date='" + partition + "')";
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /** 第一批可运行的数仓加工：行为和评价明细清洗，以及日指标聚合。 */
    public Map<String, Object> buildBehaviorAndReview(String batchDate) throws Exception {
        String date = batchDate.replace("'", "");
        List<String> statements = Arrays.asList(
                "CREATE TABLE IF NOT EXISTS mall_dwd.dwd_user_behavior_event (log_id STRING,user_id STRING,product_id STRING,behavior_type STRING,source STRING,search_keyword STRING,session_id STRING,device_type STRING,ip_city STRING,product_price DECIMAL(12,2),quantity INT,behavior_time STRING,event_date STRING,event_hour INT,source_file STRING,row_number BIGINT,ingest_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_dwd.dwd_review_detail (review_id STRING,order_id STRING,user_id STRING,product_id STRING,rating INT,review_content STRING,review_time STRING,is_append INT,append_content STRING,image_count INT,praise_num INT,merchant_reply STRING,merchant_reply_time STRING,sentiment STRING,source_file STRING,row_number BIGINT,ingest_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_dwd.dwd_user_session_path (session_id STRING,user_id STRING,event_count BIGINT,behavior_path STRING,first_time STRING,last_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ads.ads_behavior_metric_daily (stat_date STRING,metric_code STRING,metric_name STRING,metric_value DECIMAL(18,2),dimension_type STRING,dimension_value STRING,rank_no INT,batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ads.ads_review_metric_daily (stat_date STRING,metric_code STRING,metric_name STRING,metric_value DECIMAL(18,2),dimension_type STRING,dimension_value STRING,rank_no INT,batch_date STRING) STORED AS TEXTFILE",
                "INSERT OVERWRITE TABLE mall_dwd.dwd_user_behavior_event PARTITION (batch_date='" + date + "') SELECT log_id,user_id,product_id,behavior_type,source,search_keyword,session_id,device_type,ip_city,product_price,quantity,behavior_time,substr(behavior_time,1,10),cast(substr(behavior_time,12,2) as int),source_file,row_number,ingest_time FROM mall_ods.ods_user_behavior_log WHERE batch_date='" + date + "' AND log_id IS NOT NULL AND user_id IS NOT NULL",
                "INSERT OVERWRITE TABLE mall_dwd.dwd_review_detail PARTITION (batch_date='" + date + "') SELECT review_id,order_id,user_id,product_id,rating,review_content,review_time,is_append,append_content,image_count,praise_num,merchant_reply,merchant_reply_time,CASE WHEN rating>=4 THEN '正面' WHEN rating<=2 THEN '负面' ELSE '中性' END,source_file,row_number,ingest_time FROM mall_ods.ods_user_review WHERE batch_date='" + date + "' AND review_id IS NOT NULL AND rating BETWEEN 1 AND 5",
                "INSERT OVERWRITE TABLE mall_dwd.dwd_user_session_path PARTITION (batch_date='" + date + "') SELECT session_id,max(user_id),count(*),concat_ws('>',collect_list(behavior_type)),min(behavior_time),max(behavior_time) FROM mall_dwd.dwd_user_behavior_event WHERE batch_date='" + date + "' AND session_id IS NOT NULL GROUP BY session_id",
                "INSERT OVERWRITE TABLE mall_ads.ads_behavior_metric_daily SELECT '" + date + "','PV','行为量',cast(count(*) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_user_behavior_event WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','UV','访客数',cast(count(distinct user_id) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_user_behavior_event WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','BEHAVIOR','行为分布',cast(count(*) as decimal(18,2)),'BEHAVIOR',behavior_type,0,'" + date + "' FROM mall_dwd.dwd_user_behavior_event WHERE batch_date='" + date + "' GROUP BY behavior_type UNION ALL SELECT '" + date + "','SOURCE','流量来源',cast(count(*) as decimal(18,2)),'SOURCE',source,0,'" + date + "' FROM mall_dwd.dwd_user_behavior_event WHERE batch_date='" + date + "' GROUP BY source UNION ALL SELECT '" + date + "','HOUR','小时分布',cast(count(*) as decimal(18,2)),'HOUR',cast(event_hour as string),0,'" + date + "' FROM mall_dwd.dwd_user_behavior_event WHERE batch_date='" + date + "' GROUP BY event_hour UNION ALL SELECT '" + date + "','PRODUCT_TOP','热门商品',cast(count(*) as decimal(18,2)),'PRODUCT',product_id,row_number() over(order by count(*) desc),'" + date + "' FROM mall_dwd.dwd_user_behavior_event WHERE batch_date='" + date + "' GROUP BY product_id UNION ALL SELECT '" + date + "','SEARCH_WORD','搜索关键词',cast(count(*) as decimal(18,2)),'KEYWORD',search_keyword,0,'" + date + "' FROM mall_dwd.dwd_user_behavior_event WHERE batch_date='" + date + "' AND search_keyword IS NOT NULL AND search_keyword<>'' GROUP BY search_keyword",
                "INSERT OVERWRITE TABLE mall_ads.ads_review_metric_daily SELECT '" + date + "','REVIEW_COUNT','评价数',cast(count(*) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_review_detail WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','AVG_RATING','平均评分',cast(avg(rating) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_review_detail WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','SENTIMENT','情感分布',cast(count(*) as decimal(18,2)),'SENTIMENT',sentiment,0,'" + date + "' FROM mall_dwd.dwd_review_detail WHERE batch_date='" + date + "' GROUP BY sentiment UNION ALL SELECT '" + date + "','RATING','评分分布',cast(count(*) as decimal(18,2)),'RATING',cast(rating as string),0,'" + date + "' FROM mall_dwd.dwd_review_detail WHERE batch_date='" + date + "' GROUP BY rating UNION ALL SELECT '" + date + "','PRODUCT_RATING','商品口碑',cast(avg(rating) as decimal(18,2)),'PRODUCT',product_id,0,'" + date + "' FROM mall_dwd.dwd_review_detail WHERE batch_date='" + date + "' GROUP BY product_id"
        );
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(600);
            statement.execute("SET mapreduce.framework.name=yarn");
            for (String sql : statements) statement.execute(sql);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchDate", date);
        result.put("dwdTables", Arrays.asList("dwd_user_behavior_event", "dwd_review_detail"));
        result.put("adsTables", Arrays.asList("ads_behavior_metric_daily", "ads_review_metric_daily"));
        return result;
    }

    public Map<String, Object> buildSales(String batchDate) throws Exception {
        String date = batchDate.replace("'", "");
        List<String> statements = Arrays.asList(
                "CREATE TABLE IF NOT EXISTS mall_dwd.dwd_sales_order_detail (order_id STRING,user_id STRING,product_id STRING,product_name STRING,category STRING,order_status STRING,pay_amount DECIMAL(12,2),quantity INT,order_channel STRING,order_time STRING,is_return INT,return_amount DECIMAL(12,2),net_amount DECIMAL(12,2)) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ads.ads_sales_metric_daily (stat_date STRING,metric_code STRING,metric_name STRING,metric_value DECIMAL(18,2),dimension_type STRING,dimension_value STRING,rank_no INT,batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_dwd.dwd_after_sale_detail (order_id STRING,user_id STRING,product_id STRING,return_type STRING,return_reason STRING,return_amount DECIMAL(12,2),return_time STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "INSERT OVERWRITE TABLE mall_dwd.dwd_sales_order_detail PARTITION (batch_date='" + date + "') SELECT order_id,user_id,product_id,product_name,category,order_status,pay_amount,quantity,order_channel,order_time,is_return,return_amount,CASE WHEN return_amount IS NULL THEN pay_amount ELSE pay_amount-return_amount END FROM mall_ods.ods_sales_order WHERE batch_date='" + date + "' AND order_id IS NOT NULL AND pay_amount>=0",
                "INSERT OVERWRITE TABLE mall_dwd.dwd_after_sale_detail PARTITION (batch_date='" + date + "') SELECT order_id,user_id,product_id,return_type,return_reason,return_amount,return_time FROM mall_ods.ods_sales_order WHERE batch_date='" + date + "' AND is_return=1",
                "INSERT OVERWRITE TABLE mall_ads.ads_sales_metric_daily SELECT '" + date + "','ORDER_COUNT','订单数',cast(count(*) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_sales_order_detail WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','NET_SALES','净销售额',cast(sum(net_amount) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_sales_order_detail WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','AVG_ORDER_VALUE','客单价',cast(avg(net_amount) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_sales_order_detail WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','RETURN_COUNT','退换货数',cast(count(*) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_after_sale_detail WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','CATEGORY_SALES','品类销售额',cast(sum(net_amount) as decimal(18,2)),'CATEGORY',category,0,'" + date + "' FROM mall_dwd.dwd_sales_order_detail WHERE batch_date='" + date + "' GROUP BY category UNION ALL SELECT '" + date + "','CHANNEL_SALES','渠道销售额',cast(sum(net_amount) as decimal(18,2)),'CHANNEL',order_channel,0,'" + date + "' FROM mall_dwd.dwd_sales_order_detail WHERE batch_date='" + date + "' GROUP BY order_channel UNION ALL SELECT '" + date + "','STATUS_COUNT','订单状态',cast(count(*) as decimal(18,2)),'STATUS',order_status,0,'" + date + "' FROM mall_dwd.dwd_sales_order_detail WHERE batch_date='" + date + "' GROUP BY order_status"
        );
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(600);
            statement.execute("SET mapreduce.framework.name=yarn");
            for (String sql : statements) statement.execute(sql);
        }
        Map<String, Object> result = new LinkedHashMap<>(); result.put("batchDate", date); result.put("theme", "sales"); return result;
    }

    public Map<String, Object> buildService(String batchDate) throws Exception {
        String date = batchDate.replace("'", "");
        List<String> statements = Arrays.asList(
                "CREATE TABLE IF NOT EXISTS mall_dwd.dwd_service_message (chat_id STRING,session_id STRING,user_id STRING,merchant_id STRING,sender_type STRING,message_type STRING,message_content STRING,send_time STRING,intent_type STRING,first_response_seconds INT,is_resolved INT) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_ads.ads_service_metric_daily (stat_date STRING,metric_code STRING,metric_name STRING,metric_value DECIMAL(18,2),dimension_type STRING,dimension_value STRING,rank_no INT,batch_date STRING) STORED AS TEXTFILE",
                "CREATE TABLE IF NOT EXISTS mall_dwd.dwd_service_session (session_id STRING,user_id STRING,intent_type STRING,message_count BIGINT,first_response_seconds INT,is_resolved INT,started_at STRING,ended_at STRING) PARTITIONED BY (batch_date STRING) STORED AS TEXTFILE",
                "INSERT OVERWRITE TABLE mall_dwd.dwd_service_message PARTITION (batch_date='" + date + "') SELECT chat_id,session_id,user_id,merchant_id,sender_type,message_type,message_content,send_time,intent_type,first_response_seconds,is_resolved FROM mall_ods.ods_service_message WHERE batch_date='" + date + "' AND session_id IS NOT NULL",
                "INSERT OVERWRITE TABLE mall_dwd.dwd_service_session PARTITION (batch_date='" + date + "') SELECT session_id,max(user_id),max(intent_type),count(*),max(first_response_seconds),max(is_resolved),min(send_time),max(send_time) FROM mall_dwd.dwd_service_message WHERE batch_date='" + date + "' GROUP BY session_id",
                "INSERT OVERWRITE TABLE mall_ads.ads_service_metric_daily SELECT '" + date + "','SESSION_COUNT','会话数',cast(count(*) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_service_session WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','AVG_RESPONSE','平均首次响应秒数',cast(avg(first_response_seconds) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_service_session WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','RESOLVED_RATE','解决率',cast(100*avg(is_resolved) as decimal(18,2)),'ALL','全部',0,'" + date + "' FROM mall_dwd.dwd_service_session WHERE batch_date='" + date + "' UNION ALL SELECT '" + date + "','INTENT_COUNT','咨询类型',cast(count(*) as decimal(18,2)),'INTENT',intent_type,0,'" + date + "' FROM mall_dwd.dwd_service_session WHERE batch_date='" + date + "' GROUP BY intent_type"
        );
        executeLocal(statements);
        Map<String,Object> result=new LinkedHashMap<>(); result.put("batchDate",date); result.put("theme","service"); return result;
    }

    public List<Map<String,Object>> query(String sql) throws Exception {
        if (sql == null || !sql.trim().toLowerCase().startsWith("select ")) throw new IllegalArgumentException("只允许 SELECT 查询");
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection connection=open(); Statement statement=connection.createStatement(); ResultSet rs=statement.executeQuery(sql)) {
            ResultSetMetaData meta=rs.getMetaData();
            while(rs.next()) { Map<String,Object> row=new LinkedHashMap<>(); for(int i=1;i<=meta.getColumnCount();i++) row.put(meta.getColumnLabel(i),rs.getObject(i)); rows.add(row); }
        }
        return rows;
    }

    private void executeLocal(List<String> statements) throws Exception {
        try(Connection connection=open(); Statement statement=connection.createStatement()) {
            statement.execute("SET mapreduce.framework.name=yarn");
            for(String sql:statements) statement.execute(sql);
        }
    }

    private synchronized Connection open() throws Exception {
        Path jar = Path.of(properties.getDriverJar());
        if (!Files.isRegularFile(jar)) throw new IllegalStateException("未找到 Hive JDBC 驱动: " + jar);
        if (driverLoader == null) driverLoader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
        Driver driver = (Driver) driverLoader.loadClass("org.apache.hive.jdbc.HiveDriver").getDeclaredConstructor().newInstance();
        java.util.Properties connectProperties = new java.util.Properties();
        connectProperties.setProperty("user", properties.getUsername());
        connectProperties.setProperty("password", properties.getPassword() == null ? "" : properties.getPassword());
        Connection connection = driver.connect(properties.getUrl(), connectProperties);
        if (connection == null) throw new IllegalStateException("Hive JDBC 驱动拒绝连接: " + properties.getUrl());
        return connection;
    }
}

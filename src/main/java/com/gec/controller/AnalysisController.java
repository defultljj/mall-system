package com.gec.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/Analysis")
public class AnalysisController {
    @Autowired private JdbcTemplate jdbc;

    @GetMapping("/overview")
    public R overview(){
        String batch=latestBatch();
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("summary",jdbc.queryForMap("SELECT "+
                "(SELECT COALESCE(SUM(net_amount),0) FROM tbl_sales_clean_detail WHERE batch_no=?) salesAmount,"+
                "(SELECT COUNT(*) FROM tbl_sales_clean_detail WHERE batch_no=?) orderCount,"+
                "(SELECT COUNT(*) FROM tbl_member WHERE status='正常') memberCount,"+
                "(SELECT COALESCE(SUM(stock<=warn_stock),0) FROM tbl_inventory) warningCount",batch,batch));
        out.put("salesTrend",jdbc.queryForList("SELECT DATE(order_time) name,ROUND(SUM(net_amount),2) value FROM tbl_sales_clean_detail WHERE batch_no=? GROUP BY DATE(order_time) ORDER BY name",batch));
        out.put("orderStatus",jdbc.queryForList("SELECT order_status name,COUNT(*) value FROM tbl_sales_clean_detail WHERE batch_no=? GROUP BY order_status ORDER BY value DESC",batch));
        out.put("categorySales",jdbc.queryForList("SELECT category name,ROUND(SUM(net_amount),2) value FROM tbl_sales_clean_detail WHERE batch_no=? GROUP BY category ORDER BY value DESC",batch));
        out.put("topProducts",jdbc.queryForList("SELECT product_name name,SUM(quantity) value FROM tbl_sales_clean_detail WHERE batch_no=? GROUP BY product_name ORDER BY value DESC LIMIT 6",batch));
        return R.ok(out);
    }

    @GetMapping("/behavior")
    public R behavior(){
        String batch=latestBatch();
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("summary",jdbc.queryForMap("SELECT COUNT(*) pv,COUNT(DISTINCT user_id) uv,COUNT(DISTINCT session_id) sessions,SUM(behavior_type='购买') purchases FROM tbl_behavior_clean_detail WHERE batch_no=?",batch));
        out.put("trend",jdbc.queryForList("SELECT behavior_type name,COUNT(*) value FROM tbl_behavior_clean_detail WHERE batch_no=? GROUP BY behavior_type ORDER BY value DESC",batch));
        out.put("source",jdbc.queryForList("SELECT dimension_value name,metric_value value FROM tbl_behavior_metric_daily WHERE batch_no=? AND metric_code='SOURCE' ORDER BY value DESC",batch));
        out.put("funnel",funnel(batch));
        return R.ok(out);
    }

    @GetMapping("/sales")
    public R sales(){
        String batch=latestBatch();
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("summary",jdbc.queryForMap("SELECT COUNT(*) orderCount,"+
                "COALESCE(SUM(net_amount),0) salesAmount,ROUND(AVG(net_amount),2) avgOrderValue,"+
                "SUM(is_return=1) returnCount,COALESCE(SUM(return_amount),0) refundAmount FROM tbl_sales_clean_detail WHERE batch_no=?",batch));
        out.put("status",jdbc.queryForList("SELECT order_status name,COUNT(*) value FROM tbl_sales_clean_detail WHERE batch_no=? GROUP BY order_status ORDER BY value DESC",batch));
        out.put("category",jdbc.queryForList("SELECT category name,ROUND(SUM(net_amount),2) value FROM tbl_sales_clean_detail WHERE batch_no=? GROUP BY category ORDER BY value DESC",batch));
        out.put("products",jdbc.queryForList("SELECT product_name name,ROUND(SUM(net_amount),2) value,SUM(quantity) quantity FROM tbl_sales_clean_detail WHERE batch_no=? GROUP BY product_name ORDER BY value DESC LIMIT 5",batch));
        return R.ok(out);
    }

    @GetMapping("/review")
    public R review(){
        String batch=latestBatch();
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("summary",jdbc.queryForMap("SELECT COUNT(*) reviewCount,ROUND(AVG(rating),2) avgRating,SUM(sentiment='正面') positiveCount,ROUND(SUM(sentiment='正面')*100/COUNT(*),1) positiveRate,0 appendCount FROM tbl_review_clean_detail WHERE batch_no=?",batch));
        out.put("rating",jdbc.queryForList("SELECT CONCAT(rating,'星') name,COUNT(*) value FROM tbl_review_clean_detail WHERE batch_no=? GROUP BY rating ORDER BY rating DESC",batch));
        out.put("products",jdbc.queryForList("SELECT COALESCE(i.product_name,r.product_id) name,ROUND(AVG(r.rating),2) value,COUNT(*) count FROM tbl_review_clean_detail r LEFT JOIN tbl_inventory i ON i.sku_code=r.product_id WHERE r.batch_no=? GROUP BY COALESCE(i.product_name,r.product_id) ORDER BY value ASC",batch));
        out.put("latest",jdbc.queryForList("SELECT r.review_id,COALESCE(i.product_name,r.product_id) product_id,r.rating,r.review_content,r.review_time FROM tbl_review_clean_detail r LEFT JOIN tbl_inventory i ON i.sku_code=r.product_id WHERE r.batch_no=? ORDER BY r.review_time DESC LIMIT 8",batch));
        return R.ok(out);
    }

    @GetMapping("/service")
    public R service(){
        String batch=latestBatch();
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("summary",jdbc.queryForMap("SELECT COUNT(DISTINCT session_id) sessionCount,ROUND(AVG(first_response_seconds),1) avgResponseSeconds,SUM(is_resolved=1) resolvedCount,ROUND(SUM(is_resolved=1)*100/COUNT(*),1) resolvedRate,COUNT(*) messageCount FROM tbl_service_clean_detail WHERE batch_no=?",batch));
        out.put("types",jdbc.queryForList("SELECT intent_type name,COUNT(*) value FROM tbl_service_clean_detail WHERE batch_no=? GROUP BY intent_type ORDER BY value DESC",batch));
        out.put("sessions",jdbc.queryForList("SELECT session_id,user_id,intent_type,first_response_seconds,is_resolved,send_time started_at FROM tbl_service_clean_detail WHERE batch_no=? ORDER BY send_time DESC",batch));
        return R.ok(out);
    }

    @GetMapping("/batchStatus")
    public R batchStatus(){
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT batch_no,batch_date,status,started_at,finished_at,published_at FROM etl_batch ORDER BY started_at DESC LIMIT 1");
        return R.ok(rows.isEmpty()?null:rows.get(0));
    }

    @GetMapping("/batchDetail")
    public R batchDetail(){
        String batch=latestBatch(); Map<String,Object> out=new LinkedHashMap<>(); out.put("batchNo",batch);
        out.put("jobs",jdbc.queryForList("SELECT job_name,status,input_count,output_count,started_at,finished_at,error_message FROM etl_job_log WHERE batch_no=? ORDER BY id",batch));
        out.put("quality",jdbc.queryForList("SELECT check_name,expected_value,actual_value,passed FROM etl_quality_result WHERE batch_no=? ORDER BY id",batch));
        out.put("counts",jdbc.queryForMap("SELECT (SELECT COUNT(*) FROM tbl_behavior_clean_detail WHERE batch_no=?) behaviorCount,(SELECT COUNT(*) FROM tbl_sales_clean_detail WHERE batch_no=?) salesCount,(SELECT COUNT(*) FROM tbl_review_clean_detail WHERE batch_no=?) reviewCount,(SELECT COUNT(*) FROM tbl_service_clean_detail WHERE batch_no=?) serviceCount",batch,batch,batch,batch));
        return R.ok(out);
    }

    @GetMapping("/export")
    public void export(@RequestParam(defaultValue="sales") String kind, HttpServletResponse response) throws Exception {
        response.setContentType("text/csv;charset=UTF-8"); response.setHeader("Content-Disposition","attachment; filename=analysis-"+kind+".csv");
        PrintWriter out=response.getWriter(); out.write('\ufeff');
        String batch=latestBatch();
        if("behavior".equals(kind)){out.println("行为类型,数量");for(Map<String,Object> r:jdbc.queryForList("SELECT behavior_type name,COUNT(*) value FROM tbl_behavior_clean_detail WHERE batch_no=? GROUP BY behavior_type",batch))out.println(r.get("name")+","+r.get("value"));}
        else if("review".equals(kind)){out.println("评分,数量");for(Map<String,Object> r:jdbc.queryForList("SELECT rating name,COUNT(*) value FROM tbl_review_clean_detail WHERE batch_no=? GROUP BY rating",batch))out.println(r.get("name")+","+r.get("value"));}
        else if("service".equals(kind)){out.println("咨询类型,数量");for(Map<String,Object> r:jdbc.queryForList("SELECT intent_type name,COUNT(*) value FROM tbl_service_clean_detail WHERE batch_no=? GROUP BY intent_type",batch))out.println(r.get("name")+","+r.get("value"));}
        else {out.println("品类,净销售额");for(Map<String,Object> r:jdbc.queryForList("SELECT category name,ROUND(SUM(net_amount),2) value FROM tbl_sales_clean_detail WHERE batch_no=? GROUP BY category",batch))out.println(r.get("name")+","+r.get("value"));}
    }

    private List<Map<String,Object>> funnel(String batch){
        return jdbc.queryForList("SELECT stage name,value FROM ("+
                "SELECT 1 seq,'浏览' stage,COUNT(DISTINCT session_id) value FROM tbl_behavior_clean_detail WHERE batch_no=? AND behavior_type='浏览' UNION ALL "+
                "SELECT 2,'收藏',COUNT(DISTINCT session_id) FROM tbl_behavior_clean_detail WHERE batch_no=? AND behavior_type='收藏' UNION ALL "+
                "SELECT 3,'加入购物车',COUNT(DISTINCT session_id) FROM tbl_behavior_clean_detail WHERE batch_no=? AND behavior_type='加入购物车' UNION ALL "+
                "SELECT 4,'购买',COUNT(DISTINCT session_id) FROM tbl_behavior_clean_detail WHERE batch_no=? AND behavior_type='购买') f ORDER BY seq",batch,batch,batch,batch);
    }

    private String latestBatch(){
        List<String> batches=jdbc.query("SELECT batch_no FROM etl_batch WHERE status='PUBLISHED' ORDER BY published_at DESC LIMIT 1",(rs,row)->rs.getString(1));
        return batches.isEmpty()?"":batches.get(0);
    }
}

package com.gec.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 第二阶段统一分析接口：指标筛选、明细分页、下钻和导出。 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisApiController {
    private final JdbcTemplate jdbc;

    public AnalysisApiController(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @GetMapping("/metrics")
    public R metrics(@RequestParam String theme,
                     @RequestParam(required=false) String startDate,
                     @RequestParam(required=false) String endDate,
                     @RequestParam(required=false) String metricCode,
                     @RequestParam(required=false) String dimensionType,
                     @RequestParam(required=false) String batchNo) {
        String[] config=config(theme); LocalDate[] range=range(startDate,endDate); String batch=blank(batchNo); if(batch==null)batch=latestBatch();
        String sql="SELECT batch_no,stat_date,metric_code,metric_name,metric_value,dimension_type,dimension_value,rank_no FROM "+config[0]+
                " WHERE batch_no=? AND stat_date BETWEEN ? AND ? AND (? IS NULL OR metric_code=?) AND (? IS NULL OR dimension_type=?) ORDER BY stat_date,metric_code,rank_no,metric_value DESC";
        return R.ok(jdbc.queryForList(sql,batch,range[0],range[1],blank(metricCode),blank(metricCode),blank(dimensionType),blank(dimensionType)));
    }

    @GetMapping("/detail")
    public R detail(@RequestParam String theme,
                    @RequestParam(defaultValue="1") int page,
                    @RequestParam(defaultValue="10") int limit,
                    @RequestParam(required=false) String keyword,
                    @RequestParam(required=false) String batchNo) {
        String[] config=config(theme); page=Math.max(1,page); limit=Math.max(1,Math.min(100,limit));
        String batch=blank(batchNo); if(batch==null) batch=latestBatch(); String word=blank(keyword);
        String where=" WHERE batch_no=? AND (? IS NULL OR "+config[2]+" LIKE CONCAT('%',?,'%'))";
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM "+config[1]+where,Long.class,batch,word,word);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM "+config[1]+where+" ORDER BY id DESC LIMIT ? OFFSET ?",batch,word,word,limit,(page-1)*limit);
        Map<String,Object> data=new LinkedHashMap<>(); data.put("batchNo",batch); data.put("page",page); data.put("limit",limit); data.put("total",total); data.put("rows",rows);
        return R.ok(data);
    }

    @GetMapping("/export")
    public void export(@RequestParam String theme,@RequestParam(required=false) String batchNo,HttpServletResponse response) throws Exception {
        String[] config=config(theme); String batch=blank(batchNo); if(batch==null) batch=latestBatch();
        response.setContentType("text/csv;charset=UTF-8"); response.setHeader("Content-Disposition","attachment; filename="+theme+"-detail.csv");
        try(PrintWriter out=response.getWriter()) { out.write('\ufeff'); out.println(String.join(",",config[3].split("\\|"))); for(Map<String,Object> row:jdbc.queryForList("SELECT "+config[4]+" FROM "+config[1]+" WHERE batch_no=? ORDER BY id",batch)){ int i=0; for(Object value:row.values()){if(i++>0)out.print(',');out.print(csv(value));}out.println();} }
    }

    private String[] config(String theme) {
        if("behavior".equals(theme)) return new String[]{"tbl_behavior_metric_daily","tbl_behavior_clean_detail","CONCAT_WS(' ',log_id,user_id,product_id,behavior_type,session_id)","日志编号|用户|商品|行为|会话|时间","log_id,user_id,product_id,behavior_type,session_id,behavior_time"};
        if("sales".equals(theme)) return new String[]{"tbl_sales_metric_daily","tbl_sales_clean_detail","CONCAT_WS(' ',order_id,user_id,product_id,product_name,category,order_status)","订单|用户|商品|商品名|品类|状态|净金额","order_id,user_id,product_id,product_name,category,order_status,net_amount"};
        if("review".equals(theme)) return new String[]{"tbl_review_metric_daily","tbl_review_clean_detail","CONCAT_WS(' ',review_id,order_id,user_id,product_id,review_content)","评价|订单|用户|商品|评分|情感|内容","review_id,order_id,user_id,product_id,rating,sentiment,review_content"};
        if("service".equals(theme)) return new String[]{"tbl_service_metric_daily","tbl_service_clean_detail","CONCAT_WS(' ',chat_id,session_id,user_id,intent_type,message_content)","消息|会话|用户|咨询类型|响应秒数|已解决|内容","chat_id,session_id,user_id,intent_type,first_response_seconds,is_resolved,message_content"};
        throw new IllegalArgumentException("theme 仅支持 behavior、sales、review、service");
    }

    private LocalDate[] range(String start,String end) { LocalDate to=end==null||end.isBlank()?LocalDate.now():LocalDate.parse(end); LocalDate from=start==null||start.isBlank()?to.minusDays(30):LocalDate.parse(start); if(from.isAfter(to))throw new IllegalArgumentException("startDate 不能晚于 endDate"); return new LocalDate[]{from,to}; }
    private String latestBatch(){List<String> rows=jdbc.query("SELECT batch_no FROM etl_batch WHERE status='PUBLISHED' ORDER BY published_at DESC LIMIT 1",(rs,n)->rs.getString(1));return rows.isEmpty()?"":rows.get(0);}
    private String blank(String value){return value==null||value.trim().isEmpty()?null:value.trim();}
    private String csv(Object value){String text=value==null?"":String.valueOf(value);if(!text.isEmpty()&&"=+-@".indexOf(text.charAt(0))>=0)text="'"+text;return '"'+text.replace("\"","\"\"")+'"';}
}

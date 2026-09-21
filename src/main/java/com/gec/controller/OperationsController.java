package com.gec.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class OperationsController {
    @Autowired private JdbcTemplate jdbc;

    @GetMapping("/Inventory/list")
    public R inventoryList(Integer page, Integer limit, String keyword, String warehouse, String warning) {
        String where = " WHERE 1=1";
        List<Object> args = new ArrayList<>();
        if (text(keyword)) { where += " AND (sku_code LIKE ? OR product_name LIKE ?)"; args.add("%" + keyword.trim() + "%"); args.add("%" + keyword.trim() + "%"); }
        if (text(warehouse)) { where += " AND warehouse=?"; args.add(warehouse.trim()); }
        if ("1".equals(warning)) where += " AND stock <= warn_stock";
        return page("SELECT * FROM tbl_inventory" + where + " ORDER BY id", "SELECT COUNT(*) FROM tbl_inventory" + where, args, page, limit);
    }

    @GetMapping("/Inventory/stats")
    public R inventoryStats() {
        return R.ok(jdbc.queryForMap("SELECT COUNT(*) skuCount,COALESCE(SUM(stock),0) totalStock," +
                "SUM(stock<=warn_stock) warningCount,COALESCE(SUM(stock*cost_price),0) stockValue FROM tbl_inventory"));
    }

    @GetMapping("/Inventory/warehouses")
    public R inventoryWarehouses() {
        return R.ok(jdbc.queryForList("SELECT DISTINCT warehouse FROM tbl_inventory " +
                "ORDER BY FIELD(warehouse,'广州中心仓','华东上海仓','华北北京仓','西南成都仓','华中武汉仓'),warehouse", String.class));
    }

    @GetMapping("/Inventory/flows")
    public R inventoryFlows(Long inventoryId) {
        return R.ok(jdbc.queryForList("SELECT * FROM tbl_inventory_flow WHERE inventory_id=? ORDER BY id DESC LIMIT 50", inventoryId));
    }

    @PostMapping("/Inventory/adjust")
    @Transactional(rollbackFor = Exception.class)
    public R adjustInventory(Long id, Integer changeQty, String bizType, String remark) {
        if (id == null || changeQty == null || changeQty == 0) throw new RuntimeException("调整数量不能为 0");
        Map<String, Object> row = one("SELECT id,sku_code,stock FROM tbl_inventory WHERE id=? FOR UPDATE", id);
        int before = ((Number) row.get("stock")).intValue();
        int after = before + changeQty;
        if (after < 0) throw new RuntimeException("库存不足，本次调整后不能小于 0");
        jdbc.update("UPDATE tbl_inventory SET stock=? WHERE id=?", after, id);
        jdbc.update("UPDATE tbl_sku SET stock=?,update_date=NOW() WHERE sku_code=?", after, row.get("sku_code"));
        String flowNo = "STK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        jdbc.update("INSERT INTO tbl_inventory_flow(inventory_id,flow_no,change_qty,before_qty,after_qty,biz_type,remark) VALUES(?,?,?,?,?,?,?)",
                id, flowNo, changeQty, before, after, text(bizType) ? bizType.trim() : "人工调整", remark);
        return R.ok().put("flowNo", flowNo).put("stock", after);
    }

    @GetMapping("/Activity/list")
    public R activityList(Integer page, Integer limit, String keyword, String status) {
        String where = " WHERE 1=1"; List<Object> args = new ArrayList<>();
        if (text(keyword)) { where += " AND activity_name LIKE ?"; args.add("%" + keyword.trim() + "%"); }
        if (text(status)) { where += " AND status=?"; args.add(status.trim()); }
        return page("SELECT * FROM tbl_activity" + where + " ORDER BY id DESC", "SELECT COUNT(*) FROM tbl_activity" + where, args, page, limit);
    }

    @PostMapping("/Activity/save")
    public R saveActivity(Long id, String activityName, String activityType, Double discountValue,
                          String startTime, String endTime, Integer totalCount, String productName) {
        require(activityName, "活动名称"); require(activityType, "活动类型"); checkPeriod(startTime, endTime);
        if (discountValue == null || discountValue <= 0 || totalCount == null || totalCount < 0) throw new RuntimeException("优惠值和发放数量不合法");
        if (id == null) jdbc.update("INSERT INTO tbl_activity(activity_name,activity_type,discount_value,start_time,end_time,total_count,status,product_name) VALUES(?,?,?,?,?,?,?,?)",
                activityName.trim(), activityType.trim(), discountValue, startTime, endTime, totalCount, initialStatus(startTime, endTime), productName);
        else jdbc.update("UPDATE tbl_activity SET activity_name=?,activity_type=?,discount_value=?,start_time=?,end_time=?,total_count=?,product_name=? WHERE id=?",
                activityName.trim(), activityType.trim(), discountValue, startTime, endTime, totalCount, productName, id);
        return R.ok();
    }

    @PostMapping("/Activity/updateStatus")
    public R activityStatus(Long id, String status) {
        if (!List.of("未开始", "进行中", "已暂停", "已结束").contains(status)) throw new RuntimeException("活动状态不合法");
        if (jdbc.update("UPDATE tbl_activity SET status=? WHERE id=?", status, id) != 1) throw new RuntimeException("活动不存在");
        return R.ok();
    }

    @GetMapping("/Member/list")
    public R memberList(Integer page, Integer limit, String keyword, String level, String status) {
        String where = " WHERE 1=1"; List<Object> args = new ArrayList<>();
        if (text(keyword)) { where += " AND (user_id LIKE ? OR nickname LIKE ? OR phone LIKE ?)"; for (int i=0;i<3;i++) args.add("%"+keyword.trim()+"%"); }
        if (text(level)) { where += " AND member_level=?"; args.add(level.trim()); }
        if (text(status)) { where += " AND status=?"; args.add(status.trim()); }
        return page("SELECT * FROM tbl_member" + where + " ORDER BY id", "SELECT COUNT(*) FROM tbl_member" + where, args, page, limit);
    }

    @GetMapping("/Member/stats")
    public R memberStats() {
        return R.ok(jdbc.queryForMap("SELECT COUNT(*) totalCount,SUM(status='黑名单') blockedCount," +
                "SUM(member_level='普通会员') normalCount,SUM(member_level='VIP会员') vipCount," +
                "SUM(member_level='黄金会员') goldCount FROM tbl_member"));
    }

    @PostMapping("/Member/updateStatus")
    public R memberStatus(Long id, String status) {
        if (!List.of("正常", "黑名单").contains(status)) throw new RuntimeException("会员状态不合法");
        if (jdbc.update("UPDATE tbl_member SET status=? WHERE id=?", status, id) != 1) throw new RuntimeException("会员不存在");
        return R.ok();
    }

    @PostMapping("/Member/adjustPoints")
    @Transactional(rollbackFor = Exception.class)
    public R adjustPoints(Long id, Integer changePoints, String remark) {
        if (id == null || changePoints == null || changePoints == 0) throw new RuntimeException("积分调整不能为 0");
        Map<String,Object> row = one("SELECT id,points FROM tbl_member WHERE id=? FOR UPDATE", id);
        int before=((Number)row.get("points")).intValue(), after=before+changePoints;
        if (after < 0) throw new RuntimeException("积分余额不足");
        jdbc.update("UPDATE tbl_member SET points=? WHERE id=?", after, id);
        jdbc.update("INSERT INTO tbl_points_flow(member_id,change_points,before_points,after_points,biz_type,remark) VALUES(?,?,?,?,?,?)",
                id, changePoints, before, after, "人工调整", remark);
        return R.ok().put("points", after);
    }

    @GetMapping("/Advertisement/list")
    public R advertisementList(Integer page, Integer limit, String keyword, String placement, String status) {
        String where=" WHERE 1=1"; List<Object> args=new ArrayList<>();
        if(text(keyword)){where+=" AND ad_name LIKE ?";args.add("%"+keyword.trim()+"%");}
        if(text(placement)){where+=" AND placement=?";args.add(placement.trim());}
        if(text(status)){where+=" AND status=?";args.add(status.trim());}
        return page("SELECT * FROM tbl_advertisement"+where+" ORDER BY weight DESC,id DESC", "SELECT COUNT(*) FROM tbl_advertisement"+where,args,page,limit);
    }

    @PostMapping("/Advertisement/save")
    public R saveAdvertisement(Long id,String adName,String placement,String imageUrl,String targetUrl,Integer weight,String startTime,String endTime){
        require(adName,"广告名称");require(placement,"投放位置");checkPeriod(startTime,endTime);
        if(weight==null||weight<0) throw new RuntimeException("投放权重不能小于 0");
        if(id==null) jdbc.update("INSERT INTO tbl_advertisement(ad_name,placement,image_url,target_url,weight,start_time,end_time,status) VALUES(?,?,?,?,?,?,?,'草稿')",adName.trim(),placement.trim(),imageUrl,targetUrl,weight,startTime,endTime);
        else jdbc.update("UPDATE tbl_advertisement SET ad_name=?,placement=?,image_url=?,target_url=?,weight=?,start_time=?,end_time=? WHERE id=?",adName.trim(),placement.trim(),imageUrl,targetUrl,weight,startTime,endTime,id);
        return R.ok();
    }

    @PostMapping("/Advertisement/updateStatus")
    public R advertisementStatus(Long id,String status){
        if(!List.of("草稿","已上架","已下架").contains(status)) throw new RuntimeException("广告状态不合法");
        if(jdbc.update("UPDATE tbl_advertisement SET status=? WHERE id=?",status,id)!=1) throw new RuntimeException("广告不存在");
        return R.ok();
    }

    private R page(String dataSql, String countSql, List<Object> args, Integer page, Integer limit) {
        int p=page==null||page<1?1:page, size=limit==null||limit<1?10:Math.min(limit,100);
        Long total=jdbc.queryForObject(countSql,Long.class,args.toArray());
        List<Object> dataArgs=new ArrayList<>(args); dataArgs.add(size); dataArgs.add((p-1)*size);
        return R.ok().put("data",jdbc.queryForList(dataSql+" LIMIT ? OFFSET ?",dataArgs.toArray())).put("total",total==null?0:total);
    }

    private Map<String,Object> one(String sql,Object... args){
        List<Map<String,Object>> rows=jdbc.queryForList(sql,args); if(rows.isEmpty()) throw new RuntimeException("数据不存在"); return rows.get(0);
    }
    private boolean text(String value){return value!=null&&!value.trim().isEmpty();}
    private void require(String value,String label){if(!text(value)) throw new RuntimeException(label+"不能为空");}
    private void checkPeriod(String start,String end){require(start,"开始时间");require(end,"结束时间");if(start.compareTo(end)>=0)throw new RuntimeException("结束时间必须晚于开始时间");}
    private String initialStatus(String start,String end){
        LocalDateTime now=LocalDateTime.now(), begin=LocalDateTime.parse(start.replace(' ','T')), finish=LocalDateTime.parse(end.replace(' ','T'));
        return now.isBefore(begin)?"未开始":(now.isAfter(finish)?"已结束":"进行中");
    }

    @ExceptionHandler(Exception.class)
    public void errors(Exception e, HttpServletResponse response){e.printStackTrace();R.err(e).write(response);}
}

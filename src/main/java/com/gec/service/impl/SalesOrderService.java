package com.gec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.dao.SalesOrderMapper;
import com.gec.domain.entity.SalesOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SalesOrderService {
    private static final List<String> STATUSES = Arrays.asList(
            "待付款", "待发货", "已发货", "已完成", "已取消");

    @Autowired
    private SalesOrderMapper orderMapper;
    @Autowired
    private JdbcTemplate jdbc;

    public IPage<SalesOrder> list(Page<SalesOrder> page, String status,
                                  String orderId, String userId, String productName,
                                  String channel, String startDate, String endDate) {
        if (hasText(startDate) && hasText(endDate) && startDate.trim().compareTo(endDate.trim()) > 0) {
            throw new RuntimeException("开始日期不能晚于结束日期");
        }
        LambdaQueryWrapper<SalesOrder> query = new LambdaQueryWrapper<>();
        if (hasText(status)) query.eq(SalesOrder::getOrderStatus, status.trim());
        if (hasText(orderId)) query.like(SalesOrder::getOrderId, orderId.trim());
        if (hasText(userId)) query.like(SalesOrder::getUserId, userId.trim());
        if (hasText(productName)) query.like(SalesOrder::getProductName, productName.trim());
        if (hasText(channel)) query.eq(SalesOrder::getOrderChannel, channel.trim());
        if (hasText(startDate)) query.ge(SalesOrder::getOrderTime, startDate.trim() + " 00:00:00");
        if (hasText(endDate)) query.le(SalesOrder::getOrderTime, endDate.trim() + " 23:59:59");
        query.orderByDesc(SalesOrder::getOrderTime);
        return orderMapper.selectPage(page, query);
    }

    public SalesOrder get(String orderId) {
        SalesOrder order = hasText(orderId) ? orderMapper.selectById(orderId) : null;
        if (order == null) throw new RuntimeException("订单不存在");
        return order;
    }

    public Map<String, Long> stats() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("全部", orderMapper.selectCount(null));
        for (String status : STATUSES) {
            result.put(status, orderMapper.selectCount(
                    new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getOrderStatus, status)));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String orderId, String targetStatus) {
        SalesOrder order = get(orderId);
        if (!isAllowed(order.getOrderStatus(), targetStatus)) {
            throw new RuntimeException("订单不能从“" + order.getOrderStatus() + "”变更为“" + targetStatus + "”");
        }
        LocalDateTime now = LocalDateTime.now();
        if ("已发货".equals(targetStatus)) ship(order);
        if ("已取消".equals(targetStatus)) releaseReservation(order);
        if ("已完成".equals(targetStatus)) settleMember(order);
        order.setOrderStatus(targetStatus);
        if ("待发货".equals(targetStatus) && order.getPayTime() == null) {
            order.setPayTime(now);
            redeemActivity(order);
            recordPurchaseBehavior(order, now);
        }
        if ("已发货".equals(targetStatus)) order.setDeliveryTime(now);
        if ("已完成".equals(targetStatus)) order.setCompleteTime(now);
        if (orderMapper.updateById(order) != 1) throw new RuntimeException("订单状态更新失败，请重试");
    }

    @Transactional(rollbackFor = Exception.class)
    public void processAfterSale(String orderId, String type, String reason, BigDecimal amount) {
        SalesOrder order = get(orderId);
        if (!"已完成".equals(order.getOrderStatus())) throw new RuntimeException("只有已完成订单可以办理售后");
        if (Integer.valueOf(1).equals(order.getIsReturn())) throw new RuntimeException("该订单已经办理过售后");
        if (!Arrays.asList("退款", "退货退款", "换货").contains(type)) throw new RuntimeException("售后类型不合法");
        BigDecimal refund = "换货".equals(type) ? BigDecimal.ZERO : (amount == null ? order.getPayAmount() : amount);
        if (refund.compareTo(BigDecimal.ZERO) < 0 || refund.compareTo(order.getPayAmount()) > 0) throw new RuntimeException("退款金额不合法");
        if ("退货退款".equals(type)) returnToStock(order, "退货入库");
        if ("换货".equals(type)) recordNoStockChange(order, "换货处理");
        if (refund.compareTo(BigDecimal.ZERO) > 0) reverseMember(order, refund);
        order.setIsReturn(1);
        order.setReturnType(type);
        order.setReturnReason(hasText(reason) ? reason.trim() : "用户申请售后");
        order.setReturnAmount(refund);
        order.setReturnTime(LocalDateTime.now());
        if (orderMapper.updateById(order) != 1) throw new RuntimeException("售后处理失败，请重试");
    }

    boolean isAllowed(String current, String target) {
        if (!STATUSES.contains(target)) return false;
        if ("待付款".equals(current)) return "待发货".equals(target) || "已取消".equals(target);
        if ("待发货".equals(current)) return "已发货".equals(target) || "已取消".equals(target);
        return "已发货".equals(current) && "已完成".equals(target);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void ship(SalesOrder order) {
        Map<String,Object> inventory = inventory(order);
        int before = number(inventory, "stock"), locked = number(inventory, "locked_stock"), qty = quantity(order);
        if (before < qty) throw new RuntimeException("库存不足，无法发货");
        long inventoryId = ((Number) inventory.get("id")).longValue();
        int after = before - qty;
        jdbc.update("UPDATE tbl_inventory SET stock=?,locked_stock=? WHERE id=?", after, Math.max(0, locked - qty), inventoryId);
        jdbc.update("UPDATE tbl_sku SET stock=?,update_date=NOW() WHERE sku_code=?", after, order.getProductId());
        flow(inventoryId, order, -qty, before, after, "销售出库", "订单发货 " + order.getOrderId());
    }

    private void releaseReservation(SalesOrder order) {
        Map<String,Object> inventory = inventory(order);
        int stock = number(inventory, "stock"), locked = number(inventory, "locked_stock"), qty = quantity(order);
        long inventoryId = ((Number) inventory.get("id")).longValue();
        jdbc.update("UPDATE tbl_inventory SET locked_stock=? WHERE id=?", Math.max(0, locked - qty), inventoryId);
        flow(inventoryId, order, 0, stock, stock, "取消释放", "订单取消释放预占库存 " + order.getOrderId());
    }

    private void settleMember(SalesOrder order) {
        List<Map<String,Object>> members = jdbc.queryForList("SELECT id,points,total_spent FROM tbl_member WHERE user_id=? FOR UPDATE", order.getUserId());
        if (members.isEmpty()) throw new RuntimeException("订单会员不存在");
        Map<String,Object> member = members.get(0);
        long memberId = ((Number) member.get("id")).longValue();
        int before = number(member, "points"), earned = order.getPayAmount().divideToIntegralValue(BigDecimal.TEN).intValue();
        BigDecimal afterSpent = ((BigDecimal) member.get("total_spent")).add(order.getPayAmount());
        jdbc.update("UPDATE tbl_member SET total_spent=?,points=?,member_level=CASE WHEN ?>=5000 THEN '黄金会员' WHEN ?>=1000 THEN 'VIP会员' ELSE '普通会员' END WHERE id=?",
                afterSpent, before + earned, afterSpent, afterSpent, memberId);
        jdbc.update("INSERT INTO tbl_points_flow(member_id,change_points,before_points,after_points,biz_type,remark) VALUES(?,?,?,?,?,?)",
                memberId, earned, before, before + earned, "订单完成", "订单 " + order.getOrderId() + " 消费返积分");
    }

    private void redeemActivity(SalesOrder order) {
        String remark = order.getRemark();
        if (!hasText(remark)) return;
        int marker = remark.indexOf("ACT-");
        if (marker < 0) return;
        int start = marker + 4, end = start;
        while (end < remark.length() && Character.isDigit(remark.charAt(end))) end++;
        if (end == start) return;
        long activityId = Long.parseLong(remark.substring(start, end));
        jdbc.update("UPDATE tbl_activity SET claimed_count=claimed_count+1 WHERE id=? AND claimed_count<total_count", activityId);
    }

    private void recordPurchaseBehavior(SalesOrder order, LocalDateTime time) {
        BigDecimal unitPrice = order.getPayAmount().add(order.getCouponAmount() == null ? BigDecimal.ZERO : order.getCouponAmount())
                .divide(BigDecimal.valueOf(quantity(order)), 2, java.math.RoundingMode.HALF_UP);
        jdbc.update("INSERT INTO tbl_behavior_event(log_id,user_id,product_id,behavior_type,source,session_id,device_type,ip_city,product_price,quantity,behavior_time) VALUES(UUID_SHORT(),?,?,?,?,?,?,?,?,?,?)",
                order.getUserId(), order.getProductId(), "购买", order.getOrderChannel(), "SESS-" + order.getOrderId(),
                "PC网页".equals(order.getOrderChannel()) ? "电脑" : "手机", "广州", unitPrice, quantity(order), time);
    }

    private void reverseMember(SalesOrder order, BigDecimal refund) {
        List<Map<String,Object>> members = jdbc.queryForList("SELECT id,points,total_spent FROM tbl_member WHERE user_id=? FOR UPDATE", order.getUserId());
        if (members.isEmpty()) throw new RuntimeException("订单会员不存在");
        Map<String,Object> member = members.get(0);
        long memberId = ((Number) member.get("id")).longValue();
        int before = number(member, "points"), deduct = refund.divideToIntegralValue(BigDecimal.TEN).intValue(), after = Math.max(0, before - deduct);
        BigDecimal afterSpent = ((BigDecimal) member.get("total_spent")).subtract(refund).max(BigDecimal.ZERO);
        jdbc.update("UPDATE tbl_member SET total_spent=?,points=?,member_level=CASE WHEN ?>=5000 THEN '黄金会员' WHEN ?>=1000 THEN 'VIP会员' ELSE '普通会员' END WHERE id=?",
                afterSpent, after, afterSpent, afterSpent, memberId);
        jdbc.update("INSERT INTO tbl_points_flow(member_id,change_points,before_points,after_points,biz_type,remark) VALUES(?,?,?,?,?,?)",
                memberId, after - before, before, after, "售后冲销", "订单 " + order.getOrderId() + " 退款冲销积分");
    }

    private void returnToStock(SalesOrder order, String bizType) {
        Map<String,Object> inventory = inventory(order);
        int before = number(inventory, "stock"), after = before + quantity(order);
        long inventoryId = ((Number) inventory.get("id")).longValue();
        jdbc.update("UPDATE tbl_inventory SET stock=? WHERE id=?", after, inventoryId);
        jdbc.update("UPDATE tbl_sku SET stock=?,update_date=NOW() WHERE sku_code=?", after, order.getProductId());
        flow(inventoryId, order, quantity(order), before, after, bizType, "订单售后 " + order.getOrderId());
    }

    private void recordNoStockChange(SalesOrder order, String bizType) {
        Map<String,Object> inventory = inventory(order);
        int stock = number(inventory, "stock");
        flow(((Number) inventory.get("id")).longValue(), order, 0, stock, stock, bizType, "订单换货 " + order.getOrderId());
    }

    private Map<String,Object> inventory(SalesOrder order) {
        if (!hasText(order.getProductId())) throw new RuntimeException("订单缺少 SKU 编码");
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT id,stock,locked_stock FROM tbl_inventory WHERE sku_code=? FOR UPDATE", order.getProductId());
        if (rows.isEmpty()) throw new RuntimeException("订单商品没有库存记录");
        return rows.get(0);
    }

    private void flow(long inventoryId, SalesOrder order, int change, int before, int after, String type, String remark) {
        String flowNo = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        jdbc.update("INSERT INTO tbl_inventory_flow(inventory_id,flow_no,change_qty,before_qty,after_qty,biz_type,remark) VALUES(?,?,?,?,?,?,?)",
                inventoryId, flowNo, change, before, after, type, remark);
    }

    private int quantity(SalesOrder order) {
        return order.getQuantity() == null || order.getQuantity() < 1 ? 1 : order.getQuantity();
    }

    private int number(Map<String,Object> row, String key) {
        Object value = row.get(key);
        if (value == null) value = row.get(toCamel(key));
        return ((Number) value).intValue();
    }

    private String toCamel(String value) {
        StringBuilder out = new StringBuilder(); boolean upper = false;
        for (char c : value.toCharArray()) { if (c == '_') upper = true; else { out.append(upper ? Character.toUpperCase(c) : c); upper = false; } }
        return out.toString();
    }
}

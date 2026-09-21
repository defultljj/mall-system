package com.gec;

import com.gec.dao.SalesOrderMapper;
import com.gec.domain.entity.SalesOrder;
import com.gec.service.impl.SalesOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {
    @Mock private SalesOrderMapper orderMapper;
    @Mock private JdbcTemplate jdbc;
    @InjectMocks private SalesOrderService orderService;

    @Test
    void pendingShipmentCanBeShipped() {
        SalesOrder order = order("A001", "待发货");
        when(orderMapper.selectById("A001")).thenReturn(order);
        when(orderMapper.updateById(order)).thenReturn(1);
        when(jdbc.queryForList("SELECT id,stock,locked_stock FROM tbl_inventory WHERE sku_code=? FOR UPDATE", "SKU-A001"))
                .thenReturn(List.of(Map.of("id", 1L, "stock", 8, "locked_stock", 1)));

        orderService.updateStatus("A001", "已发货");

        assertEquals("已发货", order.getOrderStatus());
        assertNotNull(order.getDeliveryTime());
        verify(orderMapper).updateById(order);
    }

    @Test
    void completedOrderCannotGoBackwards() {
        SalesOrder order = order("A002", "已完成");
        when(orderMapper.selectById("A002")).thenReturn(order);

        assertThrows(RuntimeException.class,
                () -> orderService.updateStatus("A002", "待发货"));
        verify(orderMapper, never()).updateById(any());
    }

    private SalesOrder order(String id, String status) {
        SalesOrder order = new SalesOrder();
        order.setOrderId(id);
        order.setOrderStatus(status);
        order.setProductId("SKU-" + id);
        order.setQuantity(1);
        return order;
    }
}

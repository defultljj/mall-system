package com.gec.controller;

import com.gec.components.FileTemplate;
import com.gec.service.impl.SalesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

@RestController
@RequestMapping("/Order")
public class SalesOrderController extends BaseController {
    @Autowired
    private SalesOrderService orderService;

    @GetMapping("/list")
    public R list(Integer page, Integer limit, String status, String orderId,
                  String userId, String productName, String channel,
                  String startDate, String endDate) {
        return R.convertPage(orderService.list(newPage(page, limit), status, orderId,
                userId, productName, channel, startDate, endDate));
    }

    @GetMapping("/get")
    public R get(String orderId) {
        return R.ok(orderService.get(orderId));
    }

    @GetMapping("/stats")
    public R stats() {
        return R.ok(orderService.stats());
    }

    @PostMapping("/updateStatus")
    public R updateStatus(String orderId, String status) {
        orderService.updateStatus(orderId, status);
        return R.ok();
    }

    @PostMapping("/afterSale")
    public R afterSale(String orderId, String type, String reason, BigDecimal amount) {
        orderService.processAfterSale(orderId, type, reason, amount);
        return R.ok();
    }

    @ExceptionHandler(Exception.class)
    public void exceptionFallback(Exception e, HttpServletResponse response) {
        e.printStackTrace();
        R.err(e).write(response);
    }

    @Override
    protected FileTemplate getFileTemplate() {
        return null;
    }
}

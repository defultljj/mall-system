package com.gec;

import com.gec.service.Stage2HiveService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Stage2HiveServiceTest {
    @Test
    void bootstrapDefinesAllThreeWarehouseLayersAndOdsTables() {
        List<String> sql = Stage2HiveService.bootstrapStatements();
        assertEquals(10, sql.size());
        assertTrue(sql.stream().anyMatch(x -> x.contains("CREATE DATABASE IF NOT EXISTS mall_ods")));
        assertTrue(sql.stream().anyMatch(x -> x.contains("mall_ods.ods_user_behavior_log")));
        assertTrue(sql.stream().anyMatch(x -> x.contains("mall_ods.ods_sales_order")));
        assertTrue(sql.stream().anyMatch(x -> x.contains("mall_ods.ods_user_review")));
        assertTrue(sql.stream().anyMatch(x -> x.contains("mall_ods.ods_service_message")));
    }
}

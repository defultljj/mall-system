package com.gec;

import com.gec.dao.GoodsAttrValueMapper;
import com.gec.dao.SkuMapper;
import com.gec.dao.SkuSaleAttrValueMapper;
import com.gec.dao.SpuDetailMapper;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 商品上架第 5 步 + 商品管理(9.12)的 Mapper "离线"解析冒烟测试:
 *   不启动 Spring、不连 MySQL/Redis, 只用裸 MyBatis Configuration 注册四个 Mapper 接口 ——
 *   @Select 里带 <script> 的动态 SQL 会在注册时被解析成 SqlSource,
 *   标签未闭合 / 属性引号写错 / OGNL 语法错 都会在这里立即暴露, 而不是等联调时才炸;
 *   再直接渲染两个分支的 SQL 文本, 确认 <if>/<where> 的行为与预期一致。
 * 覆盖: SpuDetailMapper(listGoods 分页联查 + getGoodsById)、GoodsAttrValueMapper.listBySpuId。
 * 范围说明:
 *   - SKU 两张表是纯 BaseMapper + 条件构造器(无自定义 SQL), 其 insert/selectList 语句由
 *     MyBatis-Plus 在 Spring 启动时注入, 裸 MyBatis Configuration 里取不到 —— 故只做"接口可注册"的冒烟;
 *   - 本测试不校验 SQL 在 MySQL 上能否执行(列名/联表/排序已用只读 SQL 在真库上核对过),
 *     也不写任何数据库/Redis 数据 —— 与"不运行应用、不动数据库数据"的约束一致。
 */
class GoodsMapperSqlTest {

    @Test
    void scriptSqlParsesAndRendersBothBranches() {
        Configuration cfg = new Configuration();
        cfg.addMapper(SpuDetailMapper.class);
        cfg.addMapper(GoodsAttrValueMapper.class);
        cfg.addMapper(SkuMapper.class);                 //纯 BaseMapper: 注册不抛异常即可
        cfg.addMapper(SkuSaleAttrValueMapper.class);

        //1.listGoods 被注册成"动态"SqlSource(<script> 里有 <if> 条件)
        MappedStatement listGoods =
                cfg.getMappedStatement("com.gec.dao.SpuDetailMapper.listGoods");
        assertNotNull(listGoods, "listGoods 未注册");
        assertTrue(listGoods.getSqlSource().getClass().getSimpleName().contains("Dynamic"),
                "listGoods 的 <script> 应解析成动态 SqlSource, 实际: "
                        + listGoods.getSqlSource().getClass().getName());

        //2.带 goodsName: 应渲染出 LIKE 模糊条件(<if> 命中)
        Map<String, Object> withName = new HashMap<>();
        withName.put("goodsName", "手机");
        BoundSql hit = listGoods.getBoundSql(withName);
        assertTrue(hit.getSql().contains("LIKE CONCAT('%', ?, '%')"),
                "带 goodsName 时应渲染 LIKE 条件, 实际 SQL:\n" + hit.getSql());

        //3.不带 goodsName(null): <where> 应整段省略, SQL 里不该出现 WHERE / LIKE
        Map<String, Object> noName = new HashMap<>();
        noName.put("goodsName", null);
        BoundSql miss = listGoods.getBoundSql(noName);
        assertFalse(miss.getSql().contains("LIKE"),
                "goodsName 为 null 时不应有 LIKE 条件, 实际 SQL:\n" + miss.getSql());
        assertFalse(miss.getSql().contains("WHERE"),
                "goodsName 为 null 时 <where> 应整段省略, 实际 SQL:\n" + miss.getSql());

        //4.两条静态注解 SQL: 注册成功 + 占位符渲染正确
        assertNotNull(cfg.getMappedStatement("com.gec.dao.SpuDetailMapper.getGoodsById"));
        assertNotNull(cfg.getMappedStatement("com.gec.dao.GoodsAttrValueMapper.listBySpuId"));
        MappedStatement byId =
                cfg.getMappedStatement("com.gec.dao.SpuDetailMapper.getGoodsById");
        Map<String, Object> idParam = new HashMap<>();
        idParam.put("id", 1);
        assertTrue(byId.getBoundSql(idParam).getSql().contains("WHERE spu.id = ?"),
                "getGoodsById 应带按主键过滤的 WHERE");
    }
}

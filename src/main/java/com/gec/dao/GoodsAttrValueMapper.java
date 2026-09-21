package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.entity.GoodsAttrValue;
import com.gec.domain.vo.GoodsAttrValueVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/*
 * 商品属性值 Mapper(tbl_goods_attr_value, 商品上架第 5 步的落库目标之一)。
 * 本表一行两义(与 GoodsAttrMapper 的注释同一约定):
 *  - spu_id IS NULL: 属性的"可选值定义"(属性维护模块维护);
 *  - spu_id 有值   : 某 SPU 实际选中的值(第 5 步落库写入, 商品管理读取/删除)。
 * 方法:
 *  - insert(BaseMapper 自带): 第 5 步把 规格参数 + 销售属性 两段缓存合并逐行写入;
 *  - listBySpuId(注解 SQL)  : 商品管理详情的属性块, 联 tbl_goods_attr 一次取全
 *                             attrName(展示) 与 attrType(前端据此分两块)。
 * 注意: 查询/删除都按 spu_id = #{spuId} 等值过滤, "可选值定义"行(spu_id 为 NULL)
 *       天然不会被命中, 属性维护数据不会受商品删除影响。
 */
public interface GoodsAttrValueMapper extends BaseMapper<GoodsAttrValue> {

    /* 按 SPU 取属性值 + 属性名/属性类型(结果列别名直接写成 GoodsAttrValueVO 属性名) */
    @Select("SELECT v.attr_id    AS attrId, " +
            "       v.attr_value AS attrValue, " +
            "       v.value_type AS valueType, " +
            "       a.attr_name  AS attrName, " +
            "       a.attr_type  AS attrType " +
            "  FROM tbl_goods_attr_value v " +
            "  LEFT JOIN tbl_goods_attr a ON a.id = v.attr_id " +
            " WHERE v.spu_id = #{spuId} " +
            " ORDER BY a.attr_type, v.attr_id")
    List<GoodsAttrValueVO> listBySpuId(
            @Param("spuId") Integer spuId);
}

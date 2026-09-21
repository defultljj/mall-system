package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.entity.SkuSaleAttrValue;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/*
 * SKU 销售属性值 Mapper(tbl_sku_sale_attr_value)。
 * 只用 BaseMapper 自带能力, 无需自定义 SQL / XML:
 *  - insert     第 5 步落库(每个 SKU 的 attrValues 逐行写)
 *  - selectList 商品管理详情(按 sku_id IN 批量取明细, 回内存按 sku_id 分组, 避免 N+1)
 *  - delete     商品管理级联删除(按 sku_id IN 清明细)
 */
public interface SkuSaleAttrValueMapper extends BaseMapper<SkuSaleAttrValue> {
    @Select("<script>SELECT sav.id,sav.spu_id AS spuId,sav.sku_id AS skuId,sav.attr_id AS attrId," +
            "sav.attr_value AS attrValue,attr.attr_name AS attrName " +
            "FROM tbl_sku_sale_attr_value sav LEFT JOIN tbl_goods_attr attr ON attr.id=sav.attr_id " +
            "WHERE sav.sku_id IN " +
            "<foreach collection='skuIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY sav.id</script>")
    List<SkuSaleAttrValue> listWithAttrName(@Param("skuIds") List<Integer> skuIds);
}

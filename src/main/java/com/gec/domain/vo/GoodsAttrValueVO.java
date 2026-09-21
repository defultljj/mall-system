package com.gec.domain.vo;

import lombok.Data;

/*
 * 商品属性值 VO(9.12 新增, 商品管理"详情"弹窗属性块的契约)。
 * 字段比实体 GoodsAttrValue 多了两个联表列:
 *  - attrName tbl_goods_attr.attr_name(展示用; 前端也拿它把 SKU 明细里的 attrId 翻成属性名)
 *  - attrType tbl_goods_attr.attr_type(1=基本属性/规格参数, 2=销售属性; 前端据此分两块展示)
 * 不继承 GoodsAttrValue: 详情只要这 5 个字段, 不需要 id/spuId;
 * 展示型 VO 也不参与任何写库, 无需 @TableName。
 */
@Data
public class GoodsAttrValueVO {
    private Integer attrId;     //attr_id 属性 id
    private String attrValue;   //attr_value 该商品在此属性上的取值
    private Integer valueType;  //value_type 1:单值 2:多值
    private String attrName;    //联表: 属性名称
    private Integer attrType;   //联表: 1=基本属性 2=销售属性
}

package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 商品属性值实体 → 对应 tbl_goods_attr_value 表。
 * 两种语义共用一张表(用 spu_id 区分):
 *  - spu_id IS NULL: 属性的"可选值定义"(维护属性时预先配置的候选项, 如 颜色: 黑/白);
 *  - spu_id 有值   : 某具体 SPU(商品)在该属性上实际选中的值。
 * 本模块(属性维护)只操作 spu_id IS NULL 的行; SPU 落库阶段才会写 spu_id 有值的行。
 * 不继承 BaseEntity: 该表没有 create_date/update_date 列。
 */
@Data
@TableName(value="tbl_goods_attr_value")
@EqualsAndHashCode(callSuper = false)
public class GoodsAttrValue {
    @TableId(type= IdType.AUTO)
    private Integer id;         //id
    private Integer spuId;      //spu_id(spu_id IS NULL = 属性可选值定义)
    private Integer attrId;     //attr_id 所属属性 id
    private Integer valueType;  //value_type 1:单值 2:多值
    private String attrValue;   //attr_value 可选值(多值型用 ';' 拼接)


}

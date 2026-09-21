package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 商品属性实体 → 对应 tbl_goods_attr 表(规格参数模块主表)。
 * 字段语义(照 9.11 资料):
 *  - attrName     属性名称(如: 屏幕尺寸/机身颜色)
 *  - categoryId   所属商品类别 id
 *  - attrType     属性类型: 1=基本属性(规格参数), 2=销售属性(下单时选的那种)
 *  - valueType    值类型: 1=单值(输入框/单选), 2=多值(可填多个, 用 ';' 拼接)
 *  - attrValue    属性值(冗余展示用; 真正的"可选值"列表在 tbl_goods_attr_value)
 *  - enable       是否启用: 1=启用 0=停用
 *  - searchEnable 是否参与检索: 1=参与 0=不参与
 * createDate/updateDate 继承自 BaseEntity, 由 MyMetaObjectHandler 自动填充。
 */
@Data
@TableName(value="tbl_goods_attr")
@EqualsAndHashCode(callSuper = false)
public class GoodsAttr extends BaseEntity {
    @TableId(type= IdType.AUTO)
    private Integer id;            //id
    private String  attrName;      //attr_name
    private Integer categoryId;    //category_id
    private Integer attrType;      //attr_type

    private Integer valueType;    //1:单值 | 2:多值
    private String  attrValue;    //attr_value

    private Integer enable;        //enable
    private Integer searchEnable;  //search_enable

    //attrGroupId (分组关系在 tbl_group_attr_relation 中间表, 不在本表)

    //create_date [In BaseEntity]
    //update_date [In BaseEntity]
}

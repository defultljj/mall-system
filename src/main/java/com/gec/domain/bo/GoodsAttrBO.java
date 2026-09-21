package com.gec.domain.bo;

import com.gec.domain.entity.BaseEntity;
import com.gec.domain.entity.GoodsAttrValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 规格属性分页列表 BO(9.11 资料原样): GoodsAttrMapper.xml 联表查询的承载对象。
 * 在实体字段之外多出:
 *  - attrTypeName / valueTypeName: 由下方 getter 现场翻译(基本属性|销售属性 / 单值型|多值型);
 *  - attrGroupId / groupName: 来自 group_attr_relation + goods_attr_group 的联表结果;
 *  - attrValue: 两种来源 —— 主表冗余值, 或列表查询后由"值表可选值"反填(见 setAttrValue 重载)。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GoodsAttrBO extends BaseEntity {
    private Integer id;           //id
    private String attrName;      //attr_name

    private Integer attrType;      //1:基本属性 2:销售属性
    private String  attrTypeName;  //[BO Attr]

    private Integer valueType;      //1:单值 | 2:多值
    private String  valueTypeName;  //单值 | 多值

    private String  attrValue;     /* 属性值.   */
    private Integer enable;        /* 激活使用. */
    private Integer categoryId;    /* 类别 ID   */

    private Integer searchEnable;   //

    private Integer attrGroupId; //RA.attr_group_id
    private String  groupName;   //g.group_name

    public GoodsAttrBO() {}

    public String getAttrTypeName(){
        if( attrType==null ) return null;
        String typeName = (this.attrType==1) ? "基本属性" : "销售属性";
        return typeName;
    }

    public String getValueTypeName(){
        if( valueType==null ) return null;
        String typeName = (this.valueType==1) ? "单值型" : "多值型";
        return typeName;
    }

    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }

    /* 重载: 从"值表"的可选值实体反填 —— valueType/valueTypeName 一并带出, 省一次手工赋值 */
    public void setAttrValue(GoodsAttrValue val) {
        this.valueType = val.getValueType();
        this.valueTypeName = (valueType==1) ? "单值" : "多值";
        this.attrValue = val.getAttrValue();
        System.out.printf("type:%s, value:%s\n",
                valueTypeName, attrValue);
    }

    /*
        //有了映射器, 以下构造器可以废除..
    //    public GoodsAttrBO(GoodsAttr attr) {
    //        this.id = attr.getId();
    //        this.attrType = attr.getAttrType();
    //        this.attrName = attr.getAttrName();
    //        this.attrTypeName = (attrType==1) ? "基本属性" : "销售属性";
    //        this.enable = attr.getEnable();
    //        this.searchEnable = attr.getSearchEnable();
    //        this.categoryId = attr.getCategoryId();
    //    }
    */
}

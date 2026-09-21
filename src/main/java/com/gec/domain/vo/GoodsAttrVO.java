package com.gec.domain.vo;

import com.gec.domain.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 规格属性表单 VO(9.11 资料原样):
 *  - 前端"新增/修改属性"提交的字段集合(比实体多了 attrGroupId 分组 id);
 *  - attrValue 是可选值文本(多值型用 ';' 拼接), 服务层会拆到值表;
 *  - attrGroupId 非空时, 服务层会维护属性与分组的关系。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GoodsAttrVO extends BaseEntity {
    private Integer id;           //id
    private String attrName;      //attr_name
    private Integer attrType;     //1:基本属性 2:销售属性

    private Integer valueType;    //1:单值 | 2:多值
    private String attrValue;     //attr_value

    private Integer enable;         //
    private Integer categoryId;     //
    private Integer attrGroupId;    //属性所属分组 id(写关系表用)
    private Integer searchEnable;   //

}

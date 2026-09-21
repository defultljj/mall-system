package com.gec.domain.search;

import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 规格属性列表查询条件(9.11 资料):
 *  - attrName   属性名称, 模糊匹配(LIKE);
 *  - categoryId 类别 id, 精确匹配;
 *  - attrType   属性类型(1 基本属性 | 2 销售属性), 精确匹配;
 *  - attrGroupId 属性分组 id, 精确匹配(走关系表联查)。
 * 值为 null 的条件在 GoodsAttrMapper.xml 里自动跳过。
 *
 * [清理] 资料文件 import 了 BaseEntity 但本类并未继承它, 已删除。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GoodsAttrSearch {
    private String  attrName;     //1.属性名称
    private Integer categoryId;
    private Integer attrType;
    private Integer attrGroupId;  //2.属性分组ID
}

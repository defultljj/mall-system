package com.gec.domain.search;

import lombok.Data;

/*
 * 品牌搜索条件(与表单绑定, 驼峰字段直接对应前端参数):
 *  - id:        精确匹配(id 精确)
 *  - brandName: 模糊匹配(like)
 *  - categoryId: 有值时表示"只看已关联该类别"的品牌(中间表过滤)
 */
@Data
public class BrandSearch {
    private Integer id;          //id 精确
    private String brandName;    //brandName 模糊
    private Integer categoryId;  //categoryId 过滤
}

package com.gec.domain.vo;

import com.gec.domain.entity.Brand;
import lombok.Data;

import java.util.List;

/*
 * 类别关联品牌提交体(镜像 BrandCategoryVO, 视角互换):
 *  - categoryId: 被操作的类别 id
 *  - categoryName: 冗余展示(后端以类别表真实值为准)
 *  - brands:    本次要"新增关联"的品牌列表(元素只用到 id / brandName)
 *  - deleteIds: 本次要"撤销关联"的品牌 id 列表
 */
@Data
public class CategoryBrandVO {
    private Integer categoryId;
    private String categoryName;

    //勾选的品牌(元素只取 id + brandName, 插入中间表冗余列用)
    private List<Brand> brands;
    //撤销的品牌 id(已有关联里被取消勾选的)
    private List<Integer> deleteIds;
}

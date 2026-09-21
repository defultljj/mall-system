package com.gec.domain.vo;

import com.gec.domain.entity.GoodsCategory;
import lombok.Data;

import java.util.List;

/*
 * 品牌关联类别提交体(前端 JSON body → @RequestBody):
 *  - brandId:   被操作的品牌 id
 *  - brandName: 冗余展示(后端以品牌表真实值为准, 这里只作入参回显)
 *  - categories: 本次要"新增关联"的类别列表(元素只用到 id / categoryName)
 *  - deleteIds:  本次要"撤销关联"的类别 id 列表
 * 批次语义: 勾选保存 = 删掉 deleteIds + 补上 categories 差集。
 * 所有需要引用"类别"的入参/出参一律沿用现有 GoodsCategory(有自引用树结构),
 * 不新建 Category 类(与数据库实际表结构对齐)。
 */
@Data
public class BrandCategoryVO {
    private Integer brandId;
    private String brandName;

    //勾选的类别(元素只取 id + categoryName, 插入中间表冗余列用)
    private List<GoodsCategory> categories;
    //撤销的类别 id(已有关联里被取消勾选的)
    private List<Integer> deleteIds;
}

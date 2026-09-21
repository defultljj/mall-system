package com.gec.domain.vo;

import com.gec.domain.entity.BaseEntity;
import lombok.Data;

/*
 * 品牌视图对象(列表 + 关联标记用):
 * 字段与 Brand 实体一致(真实列是 logo_name —— 资料里写 logoUrl 是笔误, 不引入),
 * 另加一个"视图字段" isAssociate:
 *  1 = 该品牌已关联了当前查询的类别, 0 = 未关联(供"按类别选品牌"页打勾用)。
 */
@Data
public class BrandVO extends BaseEntity {
    private Integer id;          //id
    private String brandName;    //brand_name 品牌名称
    private String logoName;     //logo_name 品牌 logo 文件名
    private String brandDesc;    //brand_desc 品牌描述
    private Integer showStatus;  //show_status 1=显示 0=隐藏

    //{ps}视图属性: 1=已关联 0=未关联(两段式在 Service 层打标记, 非表字段)
    private Integer isAssociate;
}

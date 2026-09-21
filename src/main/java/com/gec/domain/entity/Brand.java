package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 品牌实体 → 对应 tbl_brand 表。
 * show_status 语义: 1=显示(在商品页可选), 0=隐藏。
 */
@Data
@TableName(value="tbl_brand")
@EqualsAndHashCode(callSuper = false)
public class Brand extends BaseEntity {
    @TableId(type=IdType.AUTO)
    private Integer id;
    private String brandName;    //brand_name 品牌名称
    private String logoName;     //logo_name 品牌 logo 文件名
    private String brandDesc;    //brand_desc 品牌描述
    private Integer showStatus;  //show_status 1=显示 0=隐藏

}

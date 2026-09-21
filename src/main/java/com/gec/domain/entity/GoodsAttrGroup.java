package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 商品属性分组实体 → 对应 tbl_goods_attr_group 表。
 * [补齐] 资料包里此文件缺失, 是 dao/OptionMapper.java 的 import 暴露出来的缺口;
 *        字段与表结构以 mall_sys.sql 的 tbl_goods_attr_group 建表语句为准:
 *        id / group_name / category_id / descript / icons / sort + 公共 create_date / update_date
 * (当前用户/角色/部门模块不直接使用, 但实体类保留, 供后续商品管理模块使用)
 */
@Data
@TableName(value="tbl_goods_attr_group")
@EqualsAndHashCode(callSuper = false)
public class GoodsAttrGroup extends BaseEntity {
    @TableId(type=IdType.AUTO)
    private Integer id;
    private String groupName;     //group_name 属性组名称(如: 主体/屏幕/存储)
    private Integer categoryId;   //category_id 所属商品类别
    private String descript;      //descript 分组描述
    private String icons;         //icons 分组图标
    private Integer sort;         //sort 排序号

}
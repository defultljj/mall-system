package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 商品 SPU 详情实体 → 对应 tbl_spu_detail 表(商品上架第 1 步: 基本信息)。
 * 字段与列按"下划线转驼峰"一一对应(全局配置 map-underscore-to-camel-case=true):
 *  - main_img     商品主图文件名(只存文件名, 形如 8fc4bf2582154284.png;
 *                 展示时拼 /PublishGoods/showImg/goods/{文件名} 读图)
 *  - allow_deduct 积分设置:最多允许抵扣的积分(数值, 讲师模板默认 150)
 *  - reward_point 积分设置:返还积分(数值, 讲师模板默认 100)
 * 这三列是商品上架第 1 步新增的列(表结构已提前加好, 代码里不再做 DDL)。
 * create_date/update_date 由 config.MyMetaObjectHandler 自动填充, 不接收前端提交。
 */
@Data
@TableName(value="tbl_spu_detail")
@EqualsAndHashCode(callSuper = false)
public class SpuDetail extends BaseEntity {
    @TableId(type=IdType.AUTO)
    private Integer id;            //id 自增主键
    private String goodsName;      //goods_name 商品名称
    private String goodsDetails;   //goods_details 商品描述
    private Integer categoryId;    //category_id 所属类别 id
    private Integer brandId;       //brand_id 所属品牌 id
    private Float weight;          //weight 重量(Kg)
    private String mainImg;        //main_img 商品主图文件名(见类注释)
    private Integer allowDeduct;   //allow_deduct 最多允许抵扣积分(数值)
    private Integer rewardPoint;   //reward_point 返还积分(数值)
    private String lifecycleStatus;
}

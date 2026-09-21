package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/*
 * SKU 实体 → 对应 tbl_sku 表(商品上架第 5 步"保存完成"的落库目标之一; 9.12 新增)。
 * 一行 = 一个销售属性值组合, 由第 4 步缓存(pubKey-sku 里的 SkuItem)映射而来:
 *  - spuId   所属 SPU(tbl_spu_detail.id); 缓存行的 SkuItem 没有该字段, 落库时回填真实值
 *  - skuName 销售属性值组合串(如 "256GB;黑铂金", 前端按销售属性顺序用 ';' 拼接)
 *  - price   售价, 表列 decimal(10,2) —— 金额用 BigDecimal, 避免 double 的精度误差
 *  - stock   库存
 *  - skuImg  SKU 图片文件名(可空; 本期前端固定传 null, 图片/图集属后续资料)
 * create_date/update_date 由 config.MyMetaObjectHandler 自动填充, 不接收前端提交。
 * id 列已由本次建表设为 AUTO_INCREMENT, 故 @TableId(type=IdType.AUTO):
 * insert 后数据库生成的 skuId 会回填到实体, 供 tbl_sku_sale_attr_value.sku_id 当外键。
 */
@Data
@TableName(value="tbl_sku")
@EqualsAndHashCode(callSuper = false)
public class Sku extends BaseEntity {
    @TableId(type=IdType.AUTO)
    private Integer id;          //id 自增主键
    private Integer spuId;       //spu_id 所属 SPU id
    private String skuName;      //sku_name 销售属性值组合
    private BigDecimal price;    //price 售价
    private Integer stock;       //stock 库存
    private String skuImg;       //sku_img SKU 图片文件名
    private String skuCode;
    private String skuStatus;
    private BigDecimal discountPrice;
}

package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * SKU 销售属性值实体 → 对应 tbl_sku_sale_attr_value 表(商品上架第 5 步的落库目标之一)。
 * 一行 = 某 SKU 在某销售属性上的取值(如 SKU#20 "机身颜色=黑铂金")。
 * 商品管理详情弹窗里的"规格明细"一列就是这张表的数据(按 sku_id 分组后拼展示)。
 *
 * [本次补齐] 9.11 只建了表(备份见 docs/backup-tbl_sku_sale_attr_value-20260912.sql), 工程里
 *            一直没有对应实体/Mapper; 随第 5 步落库一并补上。
 * [主键适配] 表已补自增主键, 只读核对(CREATE TABLE)结果为:
 *              `id` int NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`)
 *            因此实体显式标注 @TableId(type=IdType.AUTO): id 交给数据库生成并回填,
 *            插入时不用(也不能)手写 id —— 否则与自增值冲突/写入 0。
 *
 * 不继承 BaseEntity: 该表没有 create_date/update_date 列(与 tbl_goods_attr_value 同理)。
 */
@Data
@TableName(value="tbl_sku_sale_attr_value")
@EqualsAndHashCode(callSuper = false)
public class SkuSaleAttrValue {
    @TableId(type=IdType.AUTO)
    private Integer id;        //id 自增主键(本次补)
    private Integer spuId;     //spu_id 所属 SPU id
    private Integer skuId;     //sku_id 所属 SKU id
    private Integer attrId;    //attr_id 销售属性 id(tbl_goods_attr.id)
    private String attrValue;  //attr_value 该 SKU 在此销售属性上的取值
    @TableField(exist = false)
    private String attrName;   //联表展示字段，不参与写库
}

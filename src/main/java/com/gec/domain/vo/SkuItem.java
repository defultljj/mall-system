package com.gec.domain.vo;

import com.gec.domain.entity.GoodsAttrValue;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/*
 * 发布商品第 4 步(SKU 设置)中单个 SKU 的 Redis 缓存载体(9.12 补全)。
 *  - skuName   : 销售属性值组合串, 如 "256GB;黑铂金"(前端按销售属性顺序用 ';' 拼接);
 *  - price     : 该 SKU 的售价(金额用 BigDecimal, 避免 double 的精度误差);
 *  - stock     : 该 SKU 的库存数;
 *  - skuImg    : 该 SKU 的图片文件名(可空, 为空时前端回退展示主图);
 *  - attrValues: 该 SKU 的销售属性明细, 元素复用实体 GoodsAttrValue
 *                (此时只有 attrId/attrValue, 尚无 spuId, 与第 2/3 步同理)。
 * 说明: 不继承 BaseEntity(讲师 VO 习惯: 只有最外层 VO 继承), 实现 Serializable
 *       以满足 Jackson 缓存序列化; 无参构造 + getter/setter 由 @Data 生成,
 *       反序列化时 Jackson 依赖它们把 JSON 还原成 SkuItem。
 */
@Data
public class SkuItem implements Serializable {
    private String skuName;
    private BigDecimal price;
    private Integer stock;
    private String skuImg;
    private List<GoodsAttrValue> attrValues;

}

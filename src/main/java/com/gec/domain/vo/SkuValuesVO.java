package com.gec.domain.vo;

import com.gec.domain.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/*
 * 发布商品第 4 步(SKU 设置)的 Redis 缓存载体(9.12 补全, 结构照第 2 步 GoodsAttrValuesVO):
 *  - pubKey  : 第 1 步生成、随请求带上来的 16 位发布流水号(第 2 步起只透传, 不再新建);
 *  - skuList : 前端按销售属性组合生成的全部 SKU 行(名称/价格/库存/图片/属性明细)。
 * 继承 BaseEntity 与第 2/3 步 VO 保持一致(带来 createDate/updateDate 两个字段)。
 * 缓存时按同样模式只存 skuList 本体, 因此序列化后 Redis 里是该列表的 JSON 数组。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SkuValuesVO extends BaseEntity {
    private String pubKey;
    private List<SkuItem> skuList;

}

package com.gec.domain.vo;

import com.gec.domain.entity.BaseEntity;
import com.gec.domain.entity.GoodsAttrValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/*
 * 发布商品第 2 步(规格参数)的 Redis 缓存载体(9.12 资料, 与讲师文件一致):
 *  - pubKey   : 第 1 步生成、随请求带上来的 16 位发布流水号(第 2 步起只透传, 不再新建);
 *  - attrList : 用户在"规格参数"页勾选的属性值列表(前端 attrForm.list),
 *               元素复用实体 GoodsAttrValue(此时只有 attrId/attrValue, 尚无 spuId)。
 * 继承 BaseEntity 是讲师原样要求(带来 createDate/updateDate 两个字段)。
 * 缓存时按讲师答案只存 attrList 本体, 因此序列化后 Redis 里是该列表的 JSON 数组。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GoodsAttrValuesVO extends BaseEntity {
    private String pubKey;
    private List<GoodsAttrValue> attrList;

}

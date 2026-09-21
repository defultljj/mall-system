package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.entity.Sku;

/*
 * SKU Mapper(tbl_sku)。
 * 只用 BaseMapper 自带能力, 无需自定义 SQL / XML:
 *  - insert    第 5 步落库(自增 id 回填, 供 SKU 属性值子表当外键)
 *  - selectList 商品管理详情(按 spu_id 取该商品的 SKU 列表)
 *  - delete    商品管理级联删除(按 spu_id 清 SKU)
 */
public interface SkuMapper extends BaseMapper<Sku> {
}

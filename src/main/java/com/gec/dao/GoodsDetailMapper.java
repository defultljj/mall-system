package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.entity.GoodsDetail;

/*
 * 商品详情 Mapper(9.11 资料原样)。
 * 本期 GoodsDetail 只作为 Redis 缓存载体, 不落库, 因此这里没有任何自定义 SQL,
 * 只保留 BaseMapper 的通用能力(与既有实现一致)。
 */
public interface GoodsDetailMapper
    extends BaseMapper<GoodsDetail> {

}

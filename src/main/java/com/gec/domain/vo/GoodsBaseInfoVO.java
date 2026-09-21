package com.gec.domain.vo;

import com.gec.domain.entity.GoodsDetail;
import com.gec.domain.entity.PointRule;
import lombok.Data;

/*
 * 发布商品第 1 步(基本信息)的 Redis 缓存载体(9.11 资料):
 * 一次请求把三块数据打包缓存 —— goodsDetail(基本信息) + pointRule(积分规则) + spuAlbumVO(图集)。
 * 序列化用 MyRedisConfig 的 myJacksonTemp(JSON), redis-cli 里可读;
 * GenericJackson2 会附带 @class 类型信息, 读回时能还原成本类实例。
 *
 * [清理] 资料文件 import 了 Category / java.util.List 但均未使用(且本工程没有 Category 实体,
 *        留着会直接编译不过), 已删除。
 */
@Data
public class GoodsBaseInfoVO
    implements java.io.Serializable{
    private GoodsDetail goodsDetail;
    private PointRule pointRule;
    private SpuAlbumVO spuAlbumVO;
}

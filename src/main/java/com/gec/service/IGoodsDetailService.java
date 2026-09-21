package com.gec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.domain.entity.GoodsDetail;
import com.gec.domain.vo.GoodsAttrValuesVO;
import com.gec.domain.vo.GoodsBaseInfoVO;
import com.gec.domain.vo.SkuValuesVO;

/*
 * 商品详情服务接口: 发布商品各步骤写 Redis 暂存 + 最后一步统一落库。
 *   第 1 步(9.11 资料): 基本信息 → saveBaseInfoCache;
 *   第 2 步(9.12 资料): 规格参数 → saveGoodsAttrValCache;
 *   第 3 步(9.12 补全): 销售属性 → saveSaleAttrValCache;
 *   第 4 步(9.12 补全): SKU 信息 → saveSkuValCache;
 *   第 5 步(9.12 补全): 保存完成 → submitPublish(四段缓存一次性落库并清缓存)。
 */
public interface IGoodsDetailService
    extends IService<GoodsDetail> {

    /** 把第 1 步基本信息(goodsDetail + pointRule + spuAlbumVO)写进 Redis, 返回本次发布的流水号 pubKey */
    String saveBaseInfoCache(GoodsBaseInfoVO biVO);

    /** 把第 2 步规格参数(attrList)按 "pubKey-goods-attr" 写进 Redis, 返回同一流水号 pubKey */
    String saveGoodsAttrValCache(GoodsAttrValuesVO gavVO);

    /** 把第 3 步销售属性(attrList)按 "pubKey-sale-attr" 写进 Redis, 返回同一流水号 pubKey */
    String saveSaleAttrValCache(GoodsAttrValuesVO gavVO);

    /** 把第 4 步 SKU 列表(skuList)按 "pubKey-sku" 写进 Redis, 返回同一流水号 pubKey */
    String saveSkuValCache(SkuValuesVO skuVO);

    /**
     * 第 5 步: 保存完成 —— 把 Redis 四段缓存(基本信息/规格参数/销售属性/SKU)一次性落库
     * (tbl_spu_detail → tbl_goods_attr_value → tbl_sku → tbl_sku_sale_attr_value),
     * 落库成功后清掉四个 KEY; 基本信息缓存缺失时抛业务异常(缓存过期, 需从第 1 步重来)。
     *
     * @param pubKey 第 1 步生成、前端一路带过来的发布流水号
     * @return 新商品的 spuId(tbl_spu_detail 自增主键)
     */
    Integer submitPublish(String pubKey);

}

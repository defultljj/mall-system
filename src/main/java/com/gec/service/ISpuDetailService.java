package com.gec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.domain.entity.SpuDetail;
import com.gec.domain.vo.GoodsEditVO;

/*
 * 商品 SPU 详情服务(商品上架第 1 步: 基本信息 + 商品管理的删除)。
 * 继承 IService 后自带 getById / saveOrUpdate 等通用能力,
 * 本接口只声明本模块自己的方法(第 2/3 步后续在本模块继续扩展)。
 */
public interface ISpuDetailService extends IService<SpuDetail> {

    /** 保存第 1 步基本信息: id 为空 → 新增(自增 id 回填到入参对象), id 有值 → 按 id 更新 */
    void saveBaseInfo(SpuDetail spu);

    /**
     * 删除商品(级联, 商品管理 DELETE /Goods/delete 用):
     * 按 spuId 依次清 SKU 销售属性值 → SKU → 商品属性值 → SPU 主表, 全程一个事务。
     */
    void deleteGoodsCascade(Integer spuId);

    /** 完整更新商品主信息、属性值、SKU 与 SKU 销售属性。 */
    void updateGoods(GoodsEditVO form);
}

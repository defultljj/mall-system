package com.gec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.dao.GoodsAttrValueMapper;
import com.gec.dao.SkuMapper;
import com.gec.dao.SkuSaleAttrValueMapper;
import com.gec.dao.SpuDetailMapper;
import com.gec.domain.entity.GoodsAttrValue;
import com.gec.domain.entity.Sku;
import com.gec.domain.entity.SkuSaleAttrValue;
import com.gec.domain.entity.SpuDetail;
import com.gec.domain.vo.GoodsEditVO;
import com.gec.service.ISpuDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * 商品 SPU 详情服务实现。
 * saveOrUpdate 语义与品牌模块的 CRUD 风格一致:
 *  - 主键(id)为空 → insert, MyBatis-Plus 把自增 id 回填到入参对象,
 *    Controller 拿同一个对象返回给前端, 前端"下一步"直接带 id 走;
 *  - 主键有值 → updateById 更新非空字段。
 * [9.12 新增] deleteGoodsCascade: 商品管理的级联删除, 跨表写统一加 @Transactional
 *            (与 GoodsAttrServiceImpl/BrandServiceImpl 的做法一致, 事务不放在 Controller)。
 */
@Service
public class SpuDetailServiceImpl
        extends ServiceImpl<SpuDetailMapper, SpuDetail>
        implements ISpuDetailService {

    /* 级联删除要碰的三张子表(主表由本类的 ServiceImpl 基类负责) */
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private GoodsAttrValueMapper goodsAttrValueMapper;
    @Autowired
    private JdbcTemplate jdbc;

    /* 保存/更新第 1 步基本信息(id 决定 insert 还是 update) */
    @Override
    public void saveBaseInfo(SpuDetail spu) {
        saveOrUpdate(spu);
    }

    /*
     * 商品管理: 删除商品(级联清理子表)。
     * 子表之间没有数据库外键约束, 全靠代码按依赖顺序清, 否则会留下孤儿行
     * (与 BrandServiceImpl.deleteBrand 先清中间表的理由相同)。
     * 清理顺序:
     *   1.tbl_sku_sale_attr_value  先按该 SPU 的 skuIds 清明细(SKU 明细以 sku_id 为外键);
     *   2.同上, 再按 spu_id 兜底清一次(防 sku 行已丢失、明细还在的历史脏行);
     *   3.tbl_sku                  该 SPU 的 SKU 本体;
     *   4.tbl_goods_attr_value     该 SPU 的属性值 —— 注意等值条件 spu_id = 本商品,
     *                              spu_id IS NULL 的"可选值定义"行不会被命中(SQL 里 NULL = 任何值 都不成立),
     *                              所以属性维护模块的数据是安全的, 这里不必额外加条件;
     *   5.tbl_spu_detail           最后删主表。
     * 全程一个事务: 中途失败整体回滚, 不会删一半留下坏数据(前端看到的仍是完整商品)。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGoodsCascade(Integer spuId) {
        if (spuId == null) {
            throw new RuntimeException("缺少商品 id, 无法删除");
        }
        //1.先查该 SPU 的全部 SKU id: SKU 属性值明细要按 sku_id IN (...) 清
        List<Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spuId));
        List<Integer> skuIds = new ArrayList<>();
        for (Sku sku : skus) {
            if (sku.getId() != null) {
                skuIds.add(sku.getId());
            }
        }
        if (!skuIds.isEmpty()) {
            skuSaleAttrValueMapper.delete(
                    new LambdaQueryWrapper<SkuSaleAttrValue>()
                            .in(SkuSaleAttrValue::getSkuId, skuIds));
        }
        //2.兜底: sku_id 已失效但 spu_id 还指着本商品的历史脏行也一并清掉
        skuSaleAttrValueMapper.delete(
                new LambdaQueryWrapper<SkuSaleAttrValue>()
                        .eq(SkuSaleAttrValue::getSpuId, spuId));
        //3.删 SKU 本体
        skuMapper.delete(new LambdaQueryWrapper<Sku>().eq(Sku::getSpuId, spuId));
        //4.删该 SPU 的属性值(规格参数 + 销售属性两段都在这张表; NULL 定义行不受影响)
        goodsAttrValueMapper.delete(
                new LambdaQueryWrapper<GoodsAttrValue>()
                        .eq(GoodsAttrValue::getSpuId, spuId));
        //5.最后删 SPU 主表
        removeById(spuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGoods(GoodsEditVO form) {
        if (form == null || form.getSpu() == null || form.getSpu().getId() == null) {
            throw new RuntimeException("缺少商品信息");
        }
        SpuDetail input = form.getSpu();
        SpuDetail current = getById(input.getId());
        if (current == null) throw new RuntimeException("商品不存在");
        if (input.getGoodsName() == null || input.getGoodsName().trim().isEmpty()) {
            throw new RuntimeException("商品名称不能为空");
        }
        if (input.getCategoryId() == null || input.getBrandId() == null || input.getWeight() == null || input.getWeight() <= 0) {
            throw new RuntimeException("类别、品牌和重量必须填写完整");
        }
        if (input.getMainImg() == null || input.getMainImg().trim().isEmpty()) {
            throw new RuntimeException("商品主图不能为空");
        }

        current.setGoodsName(input.getGoodsName().trim());
        current.setGoodsDetails(input.getGoodsDetails());
        current.setCategoryId(input.getCategoryId());
        current.setBrandId(input.getBrandId());
        current.setWeight(input.getWeight());
        current.setMainImg(input.getMainImg().trim());
        current.setAllowDeduct(input.getAllowDeduct() == null ? 0 : input.getAllowDeduct());
        current.setRewardPoint(input.getRewardPoint() == null ? 0 : input.getRewardPoint());
        if (input.getLifecycleStatus() != null) current.setLifecycleStatus(input.getLifecycleStatus());
        updateById(current);

        goodsAttrValueMapper.delete(new LambdaQueryWrapper<GoodsAttrValue>()
                .eq(GoodsAttrValue::getSpuId, current.getId()));
        if (form.getAttrValues() != null) {
            for (GoodsEditVO.AttrValueItem item : form.getAttrValues()) {
                if (item.getAttrId() == null || item.getAttrValue() == null) continue;
                GoodsAttrValue value = new GoodsAttrValue();
                value.setSpuId(current.getId());
                value.setAttrId(item.getAttrId());
                value.setValueType(item.getValueType() == null ? 1 : item.getValueType());
                value.setAttrValue(item.getAttrValue().trim());
                goodsAttrValueMapper.insert(value);
            }
        }

        if (form.getSkus() == null || form.getSkus().isEmpty()) {
            throw new RuntimeException("商品至少需要一个 SKU");
        }
        for (GoodsEditVO.SkuItem item : form.getSkus()) {
            if (item.getId() == null || item.getPrice() == null || item.getPrice().signum() <= 0 || item.getStock() == null || item.getStock() < 0) {
                throw new RuntimeException("SKU 价格或库存不合法");
            }
            if (item.getDiscountPrice() != null && (item.getDiscountPrice().signum() <= 0 || item.getDiscountPrice().compareTo(item.getPrice()) > 0)) {
                throw new RuntimeException("SKU 优惠价必须大于 0 且不高于原价");
            }
            Sku sku = skuMapper.selectById(item.getId());
            if (sku == null || !Objects.equals(sku.getSpuId(), current.getId())) {
                throw new RuntimeException("SKU 不属于当前商品");
            }
            sku.setSkuName(item.getSkuName());
            sku.setPrice(item.getPrice());
            sku.setDiscountPrice(item.getDiscountPrice());
            sku.setStock(item.getStock());
            sku.setSkuStatus("停用".equals(item.getSkuStatus()) ? "停用" : "启用");
            sku.setSkuImg(item.getSkuImg() == null || item.getSkuImg().trim().isEmpty()
                    ? current.getMainImg() : item.getSkuImg().trim());
            skuMapper.updateById(sku);

            //库存中心与 SKU 共用同一库存数字；编码保持只读，避免历史订单关联失效。
            List<Integer> inventoryStocks = jdbc.query(
                    "SELECT stock FROM tbl_inventory WHERE id=?",
                    (rs, rowNum) -> rs.getInt(1), sku.getId());
            if (!inventoryStocks.isEmpty()) {
                int beforeStock = inventoryStocks.get(0);
                jdbc.update("UPDATE tbl_inventory SET product_name=?,stock=?,status=?,updated_at=NOW() WHERE id=?",
                        current.getGoodsName() + " " + (sku.getSkuName() == null ? "" : sku.getSkuName().replace(';', ' ')),
                        sku.getStock(), "启用".equals(sku.getSkuStatus()) ? 1 : 0, sku.getId());
                if (beforeStock != sku.getStock()) {
                    jdbc.update("INSERT INTO tbl_inventory_flow(inventory_id,flow_no,change_qty,before_qty,after_qty,biz_type,remark,created_at) " +
                                    "VALUES(?,?,?,?,?,'人工调整','商品编辑同步库存',NOW())",
                            sku.getId(), "EDIT-" + sku.getId() + "-" + System.currentTimeMillis(),
                            sku.getStock() - beforeStock, beforeStock, sku.getStock());
                }
            }

            skuSaleAttrValueMapper.delete(new LambdaQueryWrapper<SkuSaleAttrValue>()
                    .eq(SkuSaleAttrValue::getSkuId, sku.getId()));
            if (item.getAttrs() != null) {
                for (GoodsEditVO.SkuAttrItem attr : item.getAttrs()) {
                    if (attr.getAttrId() == null || attr.getAttrValue() == null) continue;
                    SkuSaleAttrValue value = new SkuSaleAttrValue();
                    value.setSpuId(current.getId());
                    value.setSkuId(sku.getId());
                    value.setAttrId(attr.getAttrId());
                    value.setAttrValue(attr.getAttrValue().trim());
                    skuSaleAttrValueMapper.insert(value);
                }
            }
        }
    }
}

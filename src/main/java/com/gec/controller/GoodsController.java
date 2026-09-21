package com.gec.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gec.components.FileTemplate;
import com.gec.dao.GoodsAttrValueMapper;
import com.gec.dao.SkuMapper;
import com.gec.dao.SkuSaleAttrValueMapper;
import com.gec.dao.SpuDetailMapper;
import com.gec.domain.entity.Sku;
import com.gec.domain.entity.SkuSaleAttrValue;
import com.gec.domain.vo.GoodsAttrValueVO;
import com.gec.domain.vo.GoodsVO;
import com.gec.domain.vo.GoodsEditVO;
import com.gec.service.ISpuDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * 商品管理(9.12 补全: 商品上架发布链路的最终闭环 —— 发布完能看、能查、能删)。
 *   list   GET    /Goods/list?page=&limit=&goodsName=  商品分页列表(goodsName 模糊可空; 联类别名/品牌名)
 *   get    GET    /Goods/get?id=                       商品详情(基础信息 + 属性值 + SKU 及销售属性明细)
 *   delete DELETE /Goods/delete?id=                    级联删除商品(属性值/SKU/SKU属性值 一并清)
 * 分工说明:
 *   - 读: 直接调 dao 的联表查询/条件构造器(表结构简单, 未单独建 GoodsMapper.xml, 见 SpuDetailMapper 注释);
 *   - 写: 删除的跨表事务放在 SpuDetailServiceImpl(项目既有约定: 事务一律落在 Service 层, Controller 只做参数与响应)。
 * 本模块不输出图片文件: 主图/缩略图仍走 /PublishGoods/showImg/goods/{fileName}, 故 getFileTemplate 返回 null
 * (与 GoodsAttrController 同一处理)。
 * 路由已被 WebConfig 登录拦截器覆盖(/Goods/** 需登录)。
 */
@RestController
@RequestMapping("/Goods")
public class GoodsController extends BaseController {

    /* 商品主表(含联表查询: 列表/详情基础信息) */
    @Autowired
    private SpuDetailMapper spuDetailMapper;
    /* 商品属性值(规格参数 + 销售属性; 详情属性块) */
    @Autowired
    private GoodsAttrValueMapper goodsAttrValueMapper;
    /* SKU 本体(详情 SKU 列表) */
    @Autowired
    private SkuMapper skuMapper;
    /* SKU 销售属性明细(详情"规格明细"列) */
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    /* 商品服务: 级联删除(事务在 Service 层) */
    @Autowired
    private ISpuDetailService spuDetailService;
    @Autowired
    private JdbcTemplate jdbc;

    /*
     * 商品分页列表(商品管理主表)。
     * GET /Goods/list?page=1&limit=10&goodsName=xx
     * goodsName 模糊可空; 联表返回 categoryName/brandName; 按 id 倒序 —— 刚发布完切过来就能看到。
     * page/limit 非法时由 newPage 回退默认 1/10; 输出统一走 R.convertPage(带 data/total)。
     */
    @GetMapping("/list")
    public R list(Integer page, Integer limit, String goodsName) {
        IPage<GoodsVO> ip = spuDetailMapper.listGoods(
                newPage(page, limit), goodsName);
        return R.convertPage(ip);
    }

    /*
     * 商品详情(前端"查看"弹窗一次取全, 不用列表行拼装)。
     * GET /Goods/get?id=1 → R.ok(Map):
     *   spu        商品主体(含类别名/品牌名, 与列表行同结构)
     *   attrValues 该商品的属性值 + 属性名/属性类型
     *              (前端按 attrType 分两块: 1=规格参数/基本属性, 2=销售属性)
     *   skus       每个 SKU = {sku: Sku 本体, attrs: 该 SKU 的销售属性明细}
     * 性能: attrs 用 sku_id IN (...) 一次批量取回, 再在内存按 sku_id 分组, 避免逐行查的 N+1。
     */
    @GetMapping("/get")
    public R get(Integer id) {
        if (id == null) {
            throw new RuntimeException("缺少商品 id");
        }
        Map<String, Object> data = new HashMap<>();
        //1.商品主体(联表: 类别名/品牌名)
        data.put("spu", spuDetailMapper.getGoodsById(id));
        //2.属性值块(联表: attrName/attrType; 排序已在 SQL 里按 attrType/attrId 固定)
        List<GoodsAttrValueVO> attrValues = goodsAttrValueMapper.listBySpuId(id);
        data.put("attrValues", attrValues);
        //3.SKU 列表 + 每个 SKU 的销售属性明细
        List<Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>()
                        .eq(Sku::getSpuId, id)
                        .orderByAsc(Sku::getId));
        List<Integer> skuIds = new ArrayList<>();
        for (Sku sku : skus) {
            if (sku.getId() != null) {
                skuIds.add(sku.getId());
            }
        }
        Map<Integer, List<SkuSaleAttrValue>> attrMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            List<SkuSaleAttrValue> all = skuSaleAttrValueMapper.listWithAttrName(skuIds);
            attrMap = all.stream()
                    .collect(Collectors.groupingBy(SkuSaleAttrValue::getSkuId));
        }
        List<Map<String, Object>> skuList = new ArrayList<>();
        for (Sku sku : skus) {
            Map<String, Object> item = new HashMap<>();
            item.put("sku", sku);
            List<SkuSaleAttrValue> attrs = attrMap.get(sku.getId());
            //没有明细也给空数组, 前端渲染逻辑不必再判 null
            item.put("attrs", attrs == null ? new ArrayList<SkuSaleAttrValue>() : attrs);
            skuList.add(item);
        }
        data.put("skus", skuList);
        return R.ok(data);
    }

    /*
     * 删除商品(DELETE; 前端已做 confirm 二次确认, 因为这是级联删除)。
     * DELETE /Goods/delete?id=1
     * 级联顺序: SKU 属性值 → SKU → 商品属性值 → SPU 主表(防孤儿行);
     * 跨表事务在 SpuDetailServiceImpl.deleteGoodsCascade —— Controller 不标 @Transactional,
     * 与项目既有约定保持一致(事务边界统一在 Service 层)。
     */
    @DeleteMapping("/delete")
    public R delete(Integer id) {
        spuDetailService.deleteGoodsCascade(id);
        return R.ok();
    }

    @PostMapping("/updateStatus")
    public R updateStatus(Integer id, String status) {
        if (!java.util.Arrays.asList("草稿", "已上架", "已下架").contains(status)) {
            throw new RuntimeException("商品状态不合法");
        }
        if (jdbc.update("UPDATE tbl_spu_detail SET lifecycle_status=?,update_date=NOW() WHERE id=?", status, id) != 1) {
            throw new RuntimeException("商品不存在");
        }
        return R.ok();
    }

    @PostMapping("/updateSku")
    public R updateSku(Integer id, String skuCode, java.math.BigDecimal price,
                       java.math.BigDecimal discountPrice, Integer stock, String status, String skuImg) {
        if (id == null || price == null || price.signum() <= 0 || stock == null || stock < 0) {
            throw new RuntimeException("SKU 价格或库存不合法");
        }
        if (!java.util.Arrays.asList("启用", "停用").contains(status)) throw new RuntimeException("SKU 状态不合法");
        if (discountPrice != null && (discountPrice.signum() <= 0 || discountPrice.compareTo(price) > 0)) {
            throw new RuntimeException("优惠价必须大于 0 且不高于原价");
        }
        if (jdbc.update("UPDATE tbl_sku SET sku_code=?,price=?,discount_price=?,stock=?,sku_status=?,sku_img=COALESCE(NULLIF(?,''),sku_img),update_date=NOW() WHERE id=?",
                skuCode, price, discountPrice, stock, status, skuImg, id) != 1) throw new RuntimeException("SKU 不存在");
        return R.ok();
    }

    @PostMapping("/update")
    public R update(@RequestBody GoodsEditVO form) {
        spuDetailService.updateGoods(form);
        return R.ok();
    }

    /* 统一异常处理(与 GoodsAttrController/BrandController 同一写法): 业务错误以 R 形式写回 */
    @ExceptionHandler(Exception.class)
    public void exceptionFallback(Exception e, HttpServletResponse resp) {
        e.printStackTrace();
        R.err(e).write(resp);
    }

    /* 本模块不直接输出图片(商品图走 /PublishGoods/showImg/goods/{fileName}), 与 GoodsAttrController 一致返回 null */
    @Override
    protected FileTemplate getFileTemplate() {
        return null;
    }
}

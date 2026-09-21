package com.gec.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.dao.GoodsAttrValueMapper;
import com.gec.dao.GoodsDetailMapper;
import com.gec.dao.SkuMapper;
import com.gec.dao.SkuSaleAttrValueMapper;
import com.gec.dao.SpuDetailMapper;
import com.gec.domain.entity.GoodsAttrValue;
import com.gec.domain.entity.GoodsDetail;
import com.gec.domain.entity.PointRule;
import com.gec.domain.entity.Sku;
import com.gec.domain.entity.SkuSaleAttrValue;
import com.gec.domain.entity.SpuDetail;
import com.gec.domain.vo.GoodsAttrValuesVO;
import com.gec.domain.vo.GoodsBaseInfoVO;
import com.gec.domain.vo.SkuItem;
import com.gec.domain.vo.SkuValuesVO;
import com.gec.service.IGoodsDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;

/*
 * 商品发布第 1 步: 基本信息写入 Redis 暂存(不落库)。
 *
 * 发布流程一共 5 步: 前 4 步(基本信息/规格属性/销售属性/SKU)都只写 Redis,
 * 第 5 步 submitPublish 才把四段缓存一次性落库并清缓存。
 * 缓存 KEY 设计(见下方注释, 与讲师的命名约定一致):
 *   KEY:"0107AX-base"        [STEP01-基本信息]  ← 本类负责
 *   KEY:"0107AX-goods-attr"  [STEP02-规格属性]
 *   KEY:"0107AX-sale-attr"   [STEP03-销售属性]
 *   KEY:"0107AX-sku"         [STEP04-SKU信息]
 * 即: pubKey 是本次发布的 16 位流水号(随机大写串), 前端带着它走完后续步骤,
 *     各步骤用"同一流水号 + 不同后缀"把同一商品的缓存片段串起来。
 *
 * 序列化: 注入 myJacksonTemp(key=字符串, value=JSON), redis-cli 里看得到明文,
 *        且 JSON 里带 @class, 读回时能还原成 GoodsBaseInfoVO。
 *
 * [清理] 资料文件里 Dept 系 import(DeptMapper/Dept/IDeptService/IBaseService)均未使用, 已删除。
 * [留白补全] saveBaseInfoCache 为讲师填空处, 按讲师答案补全; printMARK 保留原样。
 * [9.12 新增] 第 2 步 saveGoodsAttrValCache(规格参数), 照 saveBaseInfoCache 的模式实现。
 * [9.12 补全] 第 3 步 saveSaleAttrValCache(销售属性) / 第 4 步 saveSkuValCache(SKU 列表),
 *            照 saveGoodsAttrValCache 的模式实现(仅 KEY 后缀不同)。
 * [9.12 补全] 第 5 步 submitPublish(保存完成): 四段缓存一次性落库 + 清缓存, 发布链路终点。
 */
@Service
public class GoodsDetailServiceImpl
    extends ServiceImpl<GoodsDetailMapper, GoodsDetail>
    implements IGoodsDetailService {
    /* 第 5 步落库用的四个 DAO(前 4 步只碰 Redis, 到这里才第一次写 MySQL) */
    @Autowired
    private SpuDetailMapper spuDetailMapper;                 //主表: 商品基本信息
    @Autowired
    private GoodsAttrValueMapper goodsAttrValueMapper;       //属性值: 规格参数 + 销售属性
    @Autowired
    private SkuMapper skuMapper;                             //SKU 本体
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;   //SKU 的销售属性明细
    @Autowired
    private JdbcTemplate jdbc;

    /* 积分默认值: pointRule 为空时兜底(与第 1 步"直落库"通道 / 讲师模板一致: 抵扣 150, 返还 100) */
    private static final int DEFAULT_ALLOW_DEDUCT = 150;
    private static final int DEFAULT_REWARD_POINT = 100;

    @Autowired
    @Qualifier("myJacksonTemp")
    private RedisTemplate<String,Object> jacksonTemp;
    private final Map<String, Object> localDraftCache = new ConcurrentHashMap<>();

    /*
        KEY:"0107AX-base"       [STEP01-基本信息]
        KEY:"0107AX-goods-attr" [STEP02-规格属性]
        KEY:"0107AX-sale-attr"  [STEP03-销售属性]
        KEY:"0107AX-sku"        [STEP04-SKU信息]
    */
    @Override
    public String saveBaseInfoCache(GoodsBaseInfoVO biVO) {
        //2.生成 16 位发布流水号: 去掉 UUID 的横线后取前 16 位再转大写
        String pubKey = UUID.randomUUID().toString().toUpperCase().substring(0,16);
        //3.拼出第 1 步缓存 KEY: 流水号 + "-base"
        String KEY = pubKey + "-base";
        //4.写缓存(JSON 序列化; 暂不设过期时间, 与讲师一致)
        cacheSet(KEY, biVO);
        //5.打印跟踪标记(联调时肉眼确认写入的 KEY 与流水号)
        printMARK(KEY, pubKey);
        //6.流水号返回给前端, 后续步骤继续使用
        return pubKey;
    }

    /*
     * 商品发布第 2 步: 规格参数写入 Redis 暂存(9.12 讲师方案)。
     * 前端提交 GoodsAttrValuesVO = {pubKey, attrList}, 缓存 KEY = pubKey + "-goods-attr"。
     * 与第 1 步的差别: pubKey 由第 1 步生成、随请求带过来, 本步不再新建流水号, 只管写缓存。
     * 按讲师答案, OP.set 存的是 attrList 列表本体(不是整个 VO), 供最后一步落库时直接取用。
     */
    @Override
    public String saveGoodsAttrValCache(GoodsAttrValuesVO gavVO) {
        //2.取出第 1 步生成的发布流水号(第 2~4 步全程复用同一个)
        String pubKey = gavVO.getPubKey();
        //3.防御: pubKey 为空时拼出的 KEY 会退化成 "null-goods-attr" 这种脏数据,
        //  后续步骤拿真实流水号也串不起来 —— 直接拒绝, 不给发布流程埋雷
        if (pubKey == null || pubKey.trim().isEmpty()) {
            throw new RuntimeException("缺少发布键 pubKey");
        }
        //4.拼出第 2 步缓存 KEY: 流水号 + "-goods-attr"
        String KEY = pubKey + "-goods-attr";
        //5.写缓存(JSON 序列化; 讲师答案存 attrList 本体; 暂不设过期时间, 与第 1 步一致)
        cacheSet(KEY, gavVO.getAttrList());
        //6.打印跟踪标记(联调时肉眼确认写入的 KEY 与流水号)
        printMARK(KEY, pubKey);
        //7.流水号原样返回给前端, 后续步骤继续使用
        return pubKey;
    }

    /*
     * 商品发布第 3 步: 销售属性写入 Redis 暂存(9.12 补全, 照第 2 步模式)。
     * 前端提交结构同第 2 步的 GoodsAttrValuesVO = {pubKey, attrList},
     * 差别只在缓存 KEY 后缀: "-sale-attr"(第 2 步是 "-goods-attr")。
     * 同样只写 attrList 列表本体, 供最后一步落库时直接取用。
     */
    @Override
    public String saveSaleAttrValCache(GoodsAttrValuesVO gavVO) {
        //2.取出第 1 步生成的发布流水号(第 2~4 步全程复用同一个)
        String pubKey = gavVO.getPubKey();
        //3.防御: pubKey 为空时拼出的 KEY 会退化成 "null-sale-attr" 这种脏数据, 直接拒绝(同第 2 步)
        if (pubKey == null || pubKey.trim().isEmpty()) {
            throw new RuntimeException("缺少发布键 pubKey");
        }
        //4.拼出第 3 步缓存 KEY: 流水号 + "-sale-attr"
        String KEY = pubKey + "-sale-attr";
        //5.写缓存(JSON 序列化; 存 attrList 本体; 暂不设过期时间, 与第 1/2 步一致)
        cacheSet(KEY, gavVO.getAttrList());
        //6.打印跟踪标记(联调时肉眼确认写入的 KEY 与流水号)
        printMARK(KEY, pubKey);
        //7.流水号原样返回给前端, 供第 4 步继续使用
        return pubKey;
    }

    /*
     * 商品发布第 4 步: SKU 列表写入 Redis 暂存(9.12 补全, 照第 2/3 步模式)。
     * 前端提交 SkuValuesVO = {pubKey, skuList}, 缓存 KEY = pubKey + "-sku"。
     * 同样只写 skuList 列表本体, 供最后一步落库时直接取用。
     */
    @Override
    public String saveSkuValCache(SkuValuesVO skuVO) {
        //2.取出第 1 步生成的发布流水号(第 2~4 步全程复用同一个)
        String pubKey = skuVO.getPubKey();
        //3.防御: pubKey 为空时拼出的 KEY 会退化成 "null-sku" 这种脏数据, 直接拒绝(同第 2 步)
        if (pubKey == null || pubKey.trim().isEmpty()) {
            throw new RuntimeException("缺少发布键 pubKey");
        }
        //4.拼出第 4 步缓存 KEY: 流水号 + "-sku"
        String KEY = pubKey + "-sku";
        //5.写缓存(JSON 序列化; 存 skuList 本体; 暂不设过期时间, 与前几步一致)
        cacheSet(KEY, skuVO.getSkuList());
        //6.打印跟踪标记(联调时肉眼确认写入的 KEY 与流水号)
        printMARK(KEY, pubKey);
        //7.流水号原样返回给前端, 与前几步保持同一约定
        return pubKey;
    }

    /*
     * 商品发布第 5 步: 保存完成 —— 四段 Redis 缓存一次性落库, 然后清缓存(9.12 补全)。
     *
     * 落库顺序由外键依赖决定:
     *   1.tbl_spu_detail            先写主表, 拿自增 spuId(后续所有子表都以它当外键);
     *   2.tbl_goods_attr_value      规格参数 + 销售属性两段缓存合并写入;
     *   3.tbl_sku                   每个 SkuItem 一行(自增 skuId 回填);
     *   4.tbl_sku_sale_attr_value   每个 SKU 的 attrValues 逐行写入(外键用上一步回填的 skuId);
     *   5.删 Redis 四个 KEY(落库全部成功后才清 —— 失败时缓存还在, 前端可原地重试)。
     *
     * 事务: 整个方法一个事务, 任一步失败全部回滚, 不会留下"半拉子"商品
     *       (Redis 不参与事务: 若 DB 回滚, 缓存并未删除, 重新提交即可, 数据不丢)。
     * 图集: GoodsBaseInfoVO.spuAlbumVO 本期不落库 —— tbl_spu_album 尚未建表(留待后续资料),
     *       缓存里这一块随四个 KEY 一起被清理, 不做任何假装"已保存"的处理。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer submitPublish(String pubKey) {
        //0.防御: 发布键为空时拼不出 KEY, 直接拒绝(与第 2~4 步同一处理)
        if (pubKey == null || pubKey.trim().isEmpty()) {
            throw new RuntimeException("缺少发布键 pubKey");
        }

        //1.读第 1 步基本信息缓存: 必须存在 —— 没有它整条链路无从落库(spuId 都拿不到)
        String baseKey = pubKey + "-base";
        Object baseCache = cacheGet(baseKey);
        if (!(baseCache instanceof GoodsBaseInfoVO)) {
            throw new RuntimeException("发布缓存已过期，请从第 1 步重新开始");
        }
        GoodsBaseInfoVO baseInfo = (GoodsBaseInfoVO) baseCache;
        GoodsDetail goodsDetail = baseInfo.getGoodsDetail();
        if (goodsDetail == null) {
            throw new RuntimeException("发布缓存缺少商品基本信息，请从第 1 步重新开始");
        }

        //2.读第 2/3/4 步缓存: 允许缺失 —— 没配属性/没建 SKU 的商品也应能发布
        String goodsAttrKey = pubKey + "-goods-attr";
        String saleAttrKey = pubKey + "-sale-attr";
        String skuKey = pubKey + "-sku";
        List<GoodsAttrValue> goodsAttrList = readCacheList(goodsAttrKey);   //规格参数
        List<GoodsAttrValue> saleAttrList = readCacheList(saleAttrKey);     //销售属性
        List<SkuItem> skuList = readCacheList(skuKey);                      //SKU 列表

        //3.落 tbl_spu_detail(主表): mainImage→main_img、pointRule→积分两列, 自增 id 回填
        SpuDetail spu = toSpuDetail(goodsDetail, baseInfo.getPointRule());
        spuDetailMapper.insert(spu);
        Integer spuId = spu.getId();

        //4.落 tbl_goods_attr_value: 两段缓存合并逐行写(缓存行的 spuId 为 null, 落库前回填)
        int attrRows = saveAttrValues(goodsAttrList, spuId);
        attrRows += saveAttrValues(saleAttrList, spuId);

        //5.落 tbl_sku + tbl_sku_sale_attr_value
        int skuRows = 0;
        int skuAttrRows = 0;
        if (skuList != null) {
            for (SkuItem item : skuList) {
                Sku sku = new Sku();
                sku.setSpuId(spuId);                 //SkuItem 里没有 spuId, 用刚落库的主键
                sku.setSkuName(item.getSkuName());
                sku.setPrice(item.getPrice());
                sku.setStock(item.getStock());
                sku.setSkuImg(item.getSkuImg());
                skuMapper.insert(sku);               //自增 skuId 回填, 供明细子表当外键
                String skuCode = String.format("SKU-%06d", sku.getId());
                sku.setSkuCode(skuCode);
                sku.setSkuStatus("启用");
                jdbc.update("UPDATE tbl_sku SET sku_code=?,sku_status='启用' WHERE id=?", skuCode, sku.getId());
                jdbc.update("INSERT INTO tbl_inventory(sku_code,product_name,warehouse,stock,locked_stock,warn_stock,cost_price,status) VALUES(?,?,?, ?,0,10,?,1)",
                        skuCode, spu.getGoodsName() + " " + item.getSkuName(), "广州中心仓", item.getStock(), item.getPrice().multiply(new BigDecimal("0.72")));
                Long inventoryId = jdbc.queryForObject("SELECT id FROM tbl_inventory WHERE sku_code=?", Long.class, skuCode);
                jdbc.update("INSERT INTO tbl_inventory_flow(inventory_id,flow_no,change_qty,before_qty,after_qty,biz_type,remark) VALUES(?,?,?,?,?,?,?)",
                        inventoryId, "INIT-" + skuCode, item.getStock(), 0, item.getStock(), "发布入库", "商品发布初始化库存");
                skuRows++;
                skuAttrRows += saveSkuAttrValues(item.getAttrValues(), spuId, sku.getId());
            }
        }

        //6.清缓存: 四个 KEY 一次删完(前端 localStorage 三键由前端在成功响应后自行清理)
        Long cleaned = cacheDelete(Arrays.asList(baseKey, goodsAttrKey, saleAttrKey, skuKey));
        printPublishDone(pubKey, spuId, attrRows, skuRows, skuAttrRows,
                cleaned == null ? 0L : cleaned);

        //7.返回新商品 id: 前端拿它提示"发布成功! 商品 ID: xxx"并跳商品管理
        return spuId;
    }

    /*
     * 缓存里的 GoodsDetail(基本信息) + PointRule(可空) → 落库实体 SpuDetail。
     * 两处字段名/类型差异在这里一次性抹平:
     *  - mainImage(缓存字段名) → mainImg(表列 main_img);
     *  - weight 缓存里是 String(前端以 JSON 数字提交, Jackson 也能收成字符串), 表列 float;
     *  - pointRule 为空(或其中某项为空) → 用默认 150/100(与第 1 步直落库通道一致)。
     */
    private SpuDetail toSpuDetail(GoodsDetail gd, PointRule rule) {
        SpuDetail spu = new SpuDetail();
        spu.setGoodsName(gd.getGoodsName());
        spu.setGoodsDetails(gd.getGoodsDetails());
        spu.setCategoryId(gd.getCategoryId());
        spu.setBrandId(gd.getBrandId());
        spu.setMainImg(gd.getMainImage());
        //重量: 为空按 null 落库(表列可空); 格式非法直接报错, 不悄悄丢数据
        if (gd.getWeight() != null && !gd.getWeight().trim().isEmpty()) {
            try {
                spu.setWeight(Float.valueOf(gd.getWeight().trim()));
            } catch (NumberFormatException e) {
                throw new RuntimeException("商品重量格式非法: " + gd.getWeight());
            }
        }
        spu.setAllowDeduct(rule != null && rule.getAllowDeduct() != null
                ? rule.getAllowDeduct() : DEFAULT_ALLOW_DEDUCT);
        spu.setRewardPoint(rule != null && rule.getRewardPoint() != null
                ? rule.getRewardPoint() : DEFAULT_REWARD_POINT);
        return spu;
    }

    /* 一组属性值缓存 → tbl_goods_attr_value 逐行 insert; 返回写入行数(空/缺省 = 0 行) */
    private int saveAttrValues(List<GoodsAttrValue> list, Integer spuId) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        for (GoodsAttrValue val : list) {
            val.setSpuId(spuId);          //缓存行没有 spuId, 落库前回填真实值
            goodsAttrValueMapper.insert(val);
        }
        return list.size();
    }

    /* 单个 SKU 的销售属性明细 → tbl_sku_sale_attr_value 逐行 insert; 返回写入行数 */
    private int saveSkuAttrValues(List<GoodsAttrValue> list, Integer spuId, Integer skuId) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        for (GoodsAttrValue val : list) {
            SkuSaleAttrValue entity = new SkuSaleAttrValue();
            entity.setSpuId(spuId);
            entity.setSkuId(skuId);       //用第 5 步回填的 SKU 主键
            entity.setAttrId(val.getAttrId());
            entity.setAttrValue(val.getAttrValue());
            skuSaleAttrValueMapper.insert(entity);
        }
        return list.size();
    }

    /*
     * 读一段"列表"缓存(第 2/3/4 步存的都是列表本体)。
     * 为什么可以直接按泛型接住: myJacksonTemp 用的 GenericJackson2 序列化器会在 JSON 里
     * 附带 @class(列表本身 + 每个元素都带), 反序列化回来就是原来的
     * ArrayList<GoodsAttrValue> / ArrayList<SkuItem>, 元素类型不会丢。
     * 返回 null 表示该步没走过(或已过期) —— 第 5 步按"可空跳过"处理。
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> readCacheList(String key) {
        Object cache = cacheGet(key);
        if (cache == null) {
            return null;
        }
        if (!(cache instanceof List)) {
            //缓存里不是列表(多半是脏数据) —— 宁可报错让用户重走流程, 也不要写坏库
            throw new RuntimeException("发布缓存格式异常, 请从第 1 步重新开始: " + key);
        }
        return (List<T>) cache;
    }

    private void cacheSet(String key, Object value) {
        localDraftCache.put(key, value);
        try {
            jacksonTemp.opsForValue().set(key, value);
        } catch (RedisConnectionFailureException ignored) {
            System.out.println("【REDIS】未连接，商品发布草稿改用进程内暂存: " + key);
        }
    }

    private Object cacheGet(String key) {
        try {
            Object value = jacksonTemp.opsForValue().get(key);
            return value != null ? value : localDraftCache.get(key);
        } catch (RedisConnectionFailureException ignored) {
            return localDraftCache.get(key);
        }
    }

    private long cacheDelete(List<String> keys) {
        long localCount = keys.stream().filter(key -> localDraftCache.remove(key) != null).count();
        try {
            Long redisCount = jacksonTemp.delete(keys);
            return redisCount == null ? localCount : Math.max(localCount, redisCount);
        } catch (RedisConnectionFailureException ignored) {
            return localCount;
        }
    }

    /* 第 5 步的教学日志(版式沿用 printMARK, 额外报一行落库统计, 方便联调对数) */
    private void printPublishDone(String pubKey, Integer spuId,
                                  int attrRows, int skuRows, int skuAttrRows,
                                  long cleanedKeys) {
        System.out.println("+-----------------[REDIS跟踪器]-------------------+");
        System.out.println("【REDIS】发布完成, 清理缓存:..");
        System.out.println("【REDIS】pubKey:" + pubKey);
        System.out.println("【REDIS】删除 KEY 数:" + cleanedKeys);
        System.out.println("【MYSQL】落库:spuId=" + spuId
                + ", 属性值 " + attrRows + " 行, SKU " + skuRows + " 行"
                + ", SKU属性值 " + skuAttrRows + " 行");
        System.out.println("+------------------------------------------------+");
    }

    void printMARK(String KEY, String pubKey){
        System.out.println("+-----------------[REDIS跟踪器]-------------------+");
        System.out.println("【REDIS】写入 REDIS:..");
        System.out.println("【REDIS】KEY:"+ KEY);
        System.out.println("【REDIS】pubKey:"+ pubKey);
        System.out.println("+------------------------------------------------+");
    }
}

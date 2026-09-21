package com.gec;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.dao.GoodsAttrMapper;
import com.gec.domain.bo.GoodsAttrBO;
import com.gec.domain.entity.GoodsAttr;
import com.gec.domain.entity.GoodsAttrValue;
import com.gec.domain.entity.GoodsDetail;
import com.gec.domain.entity.PointRule;
import com.gec.domain.search.GoodsAttrSearch;
import com.gec.domain.vo.GoodsBaseInfoVO;
import com.gec.domain.vo.SpuAlbumVO;
import com.gec.service.IGoodsAttrService;
import com.gec.service.IGoodsDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * 9.11 Redis 集成冒烟测试(测试桩, 只读 DB + 可回滚的 Redis 键):
 *  1.jacksonTemplateRoundTrip: myJacksonTemp 对 GoodsBaseInfoVO 的序列化/反序列化往返
 *  2.jdkTemplateRoundTrip:     myJdkTemp 的基本读写
 *  3.saveBaseInfoCacheWrites:  真实走一遍发布第 1 步的 Redis 暂存(pubKey + "-base" KEY)
 *  4.goodsAttrReadOnly:        GoodsAttrMapper.xml 四条只读 SQL 冒烟(分页联查/可选值/按类别)
 * 注意: 本测试不写数据库、不改表结构; Redis 测试键用后即删。
 */
@SpringBootTest
class RedisSmokeTest {

    @Autowired
    @Qualifier("myJacksonTemp")
    private RedisTemplate<String, Object> jacksonTemp;

    @Autowired
    @Qualifier("myJdkTemp")
    private RedisTemplate<String, Object> jdkTemp;

    @Autowired
    private IGoodsDetailService detailService;

    @Autowired
    private IGoodsAttrService goodsAttrService;

    @Autowired
    private GoodsAttrMapper goodsAttrMapper;

    /* 1.JSON 模板: 复合 VO 往返, 验证序列化器与 @class 类型还原 */
    @Test
    void jacksonTemplateRoundTrip() {
        ValueOperations<String, Object> op = jacksonTemp.opsForValue();
        String key = "smoke-jackson-" + System.currentTimeMillis();

        GoodsBaseInfoVO vo = new GoodsBaseInfoVO();
        GoodsDetail detail = new GoodsDetail();
        detail.setGoodsName("冒烟测试商品");
        detail.setMainImage("smoke.png");
        vo.setGoodsDetail(detail);
        PointRule rule = new PointRule();
        rule.setRewardPoint(100);
        rule.setAllowDeduct(1);
        vo.setPointRule(rule);
        SpuAlbumVO album = new SpuAlbumVO();
        album.setImages(new String[]{"a.png", "b.png"});
        vo.setSpuAlbumVO(album);

        op.set(key, vo);
        Object back = op.get(key);
        assertTrue(back instanceof GoodsBaseInfoVO, "读回类型应为 GoodsBaseInfoVO");
        GoodsBaseInfoVO vo2 = (GoodsBaseInfoVO) back;
        assertEquals("冒烟测试商品", vo2.getGoodsDetail().getGoodsName());
        assertEquals("smoke.png", vo2.getGoodsDetail().getMainImage());
        assertEquals(100, vo2.getPointRule().getRewardPoint().intValue());
        assertArrayEquals(new String[]{"a.png", "b.png"},
                vo2.getSpuAlbumVO().getImages());

        jacksonTemp.delete(key);
        System.out.println("【冒烟通过】myJacksonTemp 写入/读回, KEY=" + key);
    }

    /* 2.JDK 模板: 键值均可读回 */
    @Test
    void jdkTemplateRoundTrip() {
        ValueOperations<String, Object> op = jdkTemp.opsForValue();
        String key = "smoke-jdk-" + System.currentTimeMillis();
        op.set(key, "jdk-serializer-ok");
        assertEquals("jdk-serializer-ok", op.get(key));
        assertEquals(Boolean.TRUE, jdkTemp.delete(key));
        System.out.println("【冒烟通过】myJdkTemp 写入/读回, KEY=" + key);
    }

    /* 3.发布第 1 步: saveBaseInfoCache 真实写 Redis, KEY=pubKey+"-base" */
    @Test
    void saveBaseInfoCacheWritesKey() {
        GoodsBaseInfoVO vo = new GoodsBaseInfoVO();
        GoodsDetail detail = new GoodsDetail();
        detail.setGoodsName("发布暂存冒烟");
        vo.setGoodsDetail(detail);

        String pubKey = detailService.saveBaseInfoCache(vo);
        assertEquals(16, pubKey.length(), "pubKey 应为 16 位");

        String key = pubKey + "-base";
        assertTrue(Boolean.TRUE.equals(jacksonTemp.hasKey(key)),
                "Redis 中应存在 " + key);
        Object back = jacksonTemp.opsForValue().get(key);
        assertTrue(back instanceof GoodsBaseInfoVO);
        assertEquals("发布暂存冒烟",
                ((GoodsBaseInfoVO) back).getGoodsDetail().getGoodsName());

        assertEquals(Boolean.TRUE, jacksonTemp.delete(key));
        System.out.println("【冒烟通过】saveBaseInfoCache, KEY=" + key);
    }

    /* 4.XML 只读冒烟: 分页联查(含分组信息) + 可选值过滤 + 按类别查询 */
    @Test
    void goodsAttrReadOnly() {
        //4.1 分页联查: 查统一演示数据的 5G 手机类别属性
        GoodsAttrSearch search = new GoodsAttrSearch();
        search.setCategoryId(111);
        IPage<GoodsAttrBO> ip = goodsAttrService.listGoodsAttr(new Page(1, 20), search);
        assertFalse(ip.getRecords().isEmpty(), "5G 手机类别应有属性数据");
        for (GoodsAttrBO bo : ip.getRecords()) {
            assertNotNull(bo.getAttrTypeName(), "attrTypeName 应由 getter 现场翻译");
            assertEquals(Integer.valueOf(111), bo.getCategoryId());
        }

        //4.2 可选值: getAttrValByAttrId 只回 spu_id IS NULL 的"定义"行
        List<GoodsAttrValue> vals = goodsAttrMapper.getAttrValByAttrId(Arrays.asList(1103, 1104));
        assertFalse(vals.isEmpty());
        for (GoodsAttrValue v : vals) {
            assertNull(v.getSpuId(), "可选值查询必须排除 spu_id 有值的行");
        }

        //4.3 listByCategory: 按类别 + 类型查(与 Controller 【5】同一个服务方法)
        List<GoodsAttr> list = goodsAttrService.queryGoodsAttrByCategory(111, 1);
        assertFalse(list.isEmpty());
        for (GoodsAttr attr : list) {
            assertEquals(Integer.valueOf(111), attr.getCategoryId());
            assertEquals(Integer.valueOf(1), attr.getAttrType());
        }
        System.out.println("【冒烟通过】GoodsAttrMapper.xml 只读 SQL: 分页联查/可选值/按类别, 共 "
                + ip.getTotal() + " 条属性");
    }
}

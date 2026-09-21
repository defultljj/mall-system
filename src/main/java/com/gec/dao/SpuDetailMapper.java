package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.domain.entity.SpuDetail;
import com.gec.domain.vo.GoodsVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/*
 * 商品 SPU 详情 Mapper。
 * 第 1 步(基本信息)只用 BaseMapper / IService 自带能力:
 * insert / updateById(自增 id 回填) / selectById, 无需自定义 SQL,
 * 也不需要 XML(与 Role/Dept 模块同一风格)。
 *
 * [9.12 新增] 商品管理模块两条联表查询(类别名/品牌名):
 *   listGoods    商品分页列表(goodsName 模糊可空, 按 id 倒序)
 *   getGoodsById 按 id 取一行(详情弹窗的基础信息块)
 * 写法决策: 只多两张 LEFT JOIN、条件只有一个可选模糊匹配 —— SQL 不复杂,
 * 用 @Select + <script> 动态条件即可, 不再单建 GoodsMapper.xml 与 GoodsMapper 接口
 * (与 OptionMapper/BrandMapper 的注解风格一致); 分页交给 MyBatis-Plus 分页插件,
 * SQL 里不写 limit。若后续商品管理复杂到需要多段 <sql> 复用, 再迁到 XML。
 * 别名约定: 不用 ur 这类两字母短名(分页插件做 count 优化时 JSqlParser 可能把短别名
 * 误判成 SQL 关键字而解析失败, 见 UserMapper.xml 的注释), 这里统一用 spu/cat/brd。
 */
public interface SpuDetailMapper extends BaseMapper<SpuDetail> {

    /* 联表查询列: 别名直接写成 GoodsVO 的属性名(与 UserMapper.xml "驼峰别名"同一约定);
       常量被下面两条 SQL 复用, 避免两处列清单写漂移 */
    String GOODS_COLS =
            "spu.id           AS id, " +
            "spu.goods_name   AS goodsName, " +
            "spu.goods_details AS goodsDetails, " +
            "spu.category_id  AS categoryId, " +
            "spu.brand_id     AS brandId, " +
            "spu.weight       AS weight, " +
            "spu.main_img     AS mainImg, " +
            "spu.allow_deduct AS allowDeduct, " +
            "spu.reward_point AS rewardPoint, " +
            "spu.lifecycle_status AS lifecycleStatus, " +
            "spu.create_date  AS createDate, " +
            "spu.update_date  AS updateDate, " +
            "cat.category_name AS categoryName, " +
            "brd.brand_name    AS brandName, " +
            "priceAgg.minPrice AS minPrice, " +
            "priceAgg.maxPrice AS maxPrice ";

    String PRICE_JOIN =
            " LEFT JOIN (SELECT spu_id, " +
            " MIN(CASE WHEN sku_status = '启用' THEN COALESCE(discount_price, price) END) AS minPrice, " +
            " MAX(CASE WHEN sku_status = '启用' THEN COALESCE(discount_price, price) END) AS maxPrice " +
            " FROM tbl_sku GROUP BY spu_id) priceAgg ON priceAgg.spu_id = spu.id ";

    /* 商品管理: 分页列表(联类别表/品牌表拿名称; goodsName 模糊可空; 最新的排前面).
       <if> 里的空串判断用 XML 单引号属性包住 Java 转义的双引号, 避免整段 SQL 满屏反斜杠 */
    @Select("<script>" +
            "SELECT " + GOODS_COLS +
            "  FROM tbl_spu_detail spu " +
            "  LEFT JOIN tbl_goods_category cat ON cat.id = spu.category_id " +
            "  LEFT JOIN tbl_brand          brd ON brd.id = spu.brand_id " +
            PRICE_JOIN +
            "<where>" +
            "  <if test='goodsName != null and goodsName != \"\"'>" +
            "    AND spu.goods_name LIKE CONCAT('%', #{goodsName}, '%') " +
            "  </if>" +
            "</where>" +
            " ORDER BY spu.id DESC" +
            "</script>")
    Page<GoodsVO> listGoods(
            Page page,
            @Param("goodsName") String goodsName);

    /* 商品管理: 按 id 取一行(带类别名/品牌名), 查不到返回 null */
    @Select("SELECT " + GOODS_COLS +
            "  FROM tbl_spu_detail spu " +
            "  LEFT JOIN tbl_goods_category cat ON cat.id = spu.category_id " +
            "  LEFT JOIN tbl_brand          brd ON brd.id = spu.brand_id " +
            PRICE_JOIN +
            " WHERE spu.id = #{id}")
    GoodsVO getGoodsById(
            @Param("id") Integer id);
}

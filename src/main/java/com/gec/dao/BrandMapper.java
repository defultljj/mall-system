package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.entity.Brand;
import com.gec.domain.vo.BrandCategoryVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/*
 * 品牌 Mapper: 品牌单表 CRUD 由 BaseMapper 提供,
 * 本类只补"品牌 ↔ 类别中间表 tbl_brand_category"的关联操作:
 *  - removeAssociate:            按品牌撤销指定类别关联(前端取消勾选)
 *  - deleteAssociatesByBrandId:  删品牌前清空它全部关联(防孤儿)
 *  - associateCategory:          品牌批量关联类别(差集由 Service 层算好再插)
 *  - selectCategoryIdsByBrand / selectBrandIdsOfCategory:
 *                               差集计算与"两段式打标记"用的纯查询
 * 关联表没有主键只有唯一键 uni_brand_category(brand_id, category_id),
 * 所以批量插入前必须先算差集, 否则会撞唯一键 —— 本轮不走 XML,
 * foreach 用 @Insert/@Delete 注解 <script> 实现。
 */
public interface BrandMapper extends BaseMapper<Brand> {

    @Select("SELECT count(1) FROM tbl_brand_category WHERE brand_id = #{id}")
    int countCategoryAssociations(@Param("id") Integer id);

    @Select("SELECT count(1) FROM tbl_spu_detail WHERE brand_id = #{id}")
    int countSpus(@Param("id") Integer id);

    //{1}撤销某品牌与指定类别们的关联: DELETE ... category_id IN (...)
    @Delete("<script>" +
            " DELETE FROM tbl_brand_category " +
            " WHERE brand_id = #{brandId} " +
            " AND category_id IN " +
            " <foreach collection='categoryIds' item='cid' open='(' separator=',' close=')'>" +
            "   #{cid}" +
            " </foreach>" +
            "</script>")
    int removeAssociate(
            @Param("brandId") Integer brandId,
            @Param("categoryIds") List<Integer> categoryIds);

    //{2}品牌批量关联类别: 一次 INSERT 多条
    //   (bcVO.categories 的元素只用到 id/categoryName, 对应中间表冗余列)
    @Insert("<script>" +
            " INSERT INTO tbl_brand_category" +
            " (brand_id, brand_name, category_id, category_name) VALUES " +
            " <foreach collection='bcVO.categories' item='c' separator=','>" +
            "   (#{bcVO.brandId}, #{bcVO.brandName}, #{c.id}, #{c.categoryName})" +
            " </foreach>" +
            "</script>")
    int associateCategory(@Param("bcVO") BrandCategoryVO bcVO);

    //{3}某品牌已关联的全部类别 id(供 Service 算差集)
    @Select(" SELECT category_id FROM tbl_brand_category WHERE brand_id = #{brandId} ")
    List<Integer> selectCategoryIdsByBrand(@Param("brandId") Integer brandId);

    //{4}按类别 + 一批品牌 id, 查其中已关联的(供"两段式"给当前页打标记)
    @Select("<script>" +
            " SELECT brand_id FROM tbl_brand_category " +
            " WHERE category_id = #{categoryId} " +
            " AND brand_id IN " +
            " <foreach collection='brandIds' item='bid' open='(' separator=',' close=')'>" +
            "   #{bid}" +
            " </foreach>" +
            "</script>")
    List<Integer> selectBrandIdsOfCategory(
            @Param("categoryId") Integer categoryId,
            @Param("brandIds") List<Integer> brandIds);

    //{5}删除品牌时清空它全部关联(防孤儿记录)
    @Delete(" DELETE FROM tbl_brand_category WHERE brand_id = #{brandId} ")
    int deleteAssociatesByBrandId(@Param("brandId") Integer brandId);
}

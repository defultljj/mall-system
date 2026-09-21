package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.entity.Brand;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.vo.CategoryBrandVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/*
 * 类别 Mapper。
 * countSub 与树形相关沿用原实现; 本轮新增的是"类别 ↔ 品牌"双向关联方法:
 *  - removeAssociate:           按类别撤销指定品牌关联(前端取消勾选)
 *  - deleteAssociatesByCategoryId: 删类别前清空它全部关联(防孤儿)
 *  - associateBrand:            类别批量关联品牌(镜像品牌侧 associateCategory)
 *  - selectBrandIdsByCategory:  某类别已关联的全部品牌 id(差集计算)
 *  - selectBrandsByCategoryId:  某类别已关联的品牌列表(前端回显, 联表)
 *  - selectByBrandId:           某品牌已关联的类别列表(品牌页回显, 联表)
 *  - getPids:                   取类别 p_ids 祖先链(如 "0,1,10")
 * 关联表唯一键 uni_brand_category(brand_id, category_id),
 * 批量插前 Service 层算差集防重; 全部用注解 SQL, 本轮不新增 XML。
 */
public interface CategoryMapper extends BaseMapper<GoodsCategory> {

    //{1}获取某类别下直属子类别的数量. (删除前业务校验用, where 前要有空格)
    @Select(" SELECT count(1) FROM tbl_goods_category WHERE parent_id = #{id} ")
    int countSub(@Param("id") Integer id);

    @Select("SELECT count(1) FROM tbl_brand_category WHERE category_id = #{id}")
    int countBrandAssociations(@Param("id") Integer id);

    @Select("SELECT count(1) FROM tbl_goods_attr_group WHERE category_id = #{id}")
    int countAttrGroups(@Param("id") Integer id);

    @Select("SELECT count(1) FROM tbl_goods_attr WHERE category_id = #{id}")
    int countAttrs(@Param("id") Integer id);

    @Select("SELECT count(1) FROM tbl_spu_detail WHERE category_id = #{id}")
    int countSpus(@Param("id") Integer id);

    //{2}获取某类别的 p_ids(祖先 id 链原始串, 如 "0,1,10"; 顶层为 "0")
    @Select(" SELECT p_ids FROM tbl_goods_category WHERE id = #{id} ")
    String getPids(@Param("id") Integer id);

    //{3}撤销某类别与指定品牌们的关联: DELETE ... brand_id IN (...)
    @Delete("<script>" +
            " DELETE FROM tbl_brand_category " +
            " WHERE category_id = #{categoryId} " +
            " AND brand_id IN " +
            " <foreach collection='brandIds' item='bid' open='(' separator=',' close=')'>" +
            "   #{bid}" +
            " </foreach>" +
            "</script>")
    int removeAssociate(
            @Param("categoryId") Integer categoryId,
            @Param("brandIds") List<Integer> brandIds);

    //{4}类别批量关联品牌: 一次 INSERT 多条
    //   (cbVO.brands 的元素只用到 id/brandName, 对应中间表冗余列)
    @Insert("<script>" +
            " INSERT INTO tbl_brand_category" +
            " (brand_id, brand_name, category_id, category_name) VALUES " +
            " <foreach collection='cbVO.brands' item='b' separator=','>" +
            "   (#{b.id}, #{b.brandName}, #{cbVO.categoryId}, #{cbVO.categoryName})" +
            " </foreach>" +
            "</script>")
    int associateBrand(@Param("cbVO") CategoryBrandVO cbVO);

    //{5}某类别下已关联的全部品牌 id(供 Service 算差集)
    @Select(" SELECT brand_id FROM tbl_brand_category WHERE category_id = #{categoryId} ")
    List<Integer> selectBrandIdsByCategory(@Param("categoryId") Integer categoryId);

    //{6}某类别已关联的品牌列表(前端回显: 只需 id/brandName, 联表去重)
    @Select(" SELECT b.id, b.brand_name " +
            " FROM tbl_brand b " +
            " JOIN tbl_brand_category bc ON b.id = bc.brand_id " +
            " WHERE bc.category_id = #{categoryId} " +
            " ORDER BY b.id ")
    List<Brand> selectBrandsByCategoryId(@Param("categoryId") Integer categoryId);

    //{7}某品牌已关联的类别列表(品牌页回显: 只需 id/categoryName, 联表去重)
    @Select(" SELECT c.id, c.category_name " +
            " FROM tbl_goods_category c " +
            " JOIN tbl_brand_category bc ON c.id = bc.category_id " +
            " WHERE bc.brand_id = #{brandId} " +
            " ORDER BY c.id ")
    List<GoodsCategory> selectByBrandId(@Param("brandId") Integer brandId);

    //{8}删除类别时清空它全部关联(防孤儿记录)
    @Delete(" DELETE FROM tbl_brand_category WHERE category_id = #{categoryId} ")
    int deleteAssociatesByCategoryId(@Param("categoryId") Integer categoryId);
}

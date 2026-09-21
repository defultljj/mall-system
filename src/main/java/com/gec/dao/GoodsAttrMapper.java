package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.domain.bo.GoodsAttrBO;
import com.gec.domain.entity.GoodsAttr;
import com.gec.domain.entity.GoodsAttrValue;
import com.gec.domain.search.GoodsAttrSearch;
import com.gec.domain.vo.GoodsAttrVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/*
 * 规格属性 Mapper(9.11 资料, 6 个方法):
 *  1.getGoodsAttrList     —— 属性 + 分组关系 + 分组名 联表分页(动态条件)
 *  2.getAttrValByAttrId   —— 按属性 id 批量取"可选值定义"(spu_id IS NULL)
 *  3.addGoodsAttrVal      —— 写属性可选值
 *  4.removeGoodsAttrVal   —— 删属性可选值(只删 spu_id IS NULL 的定义行)
 *  5.addGroupAttrRelation —— 属性 ↔ 分组 绑定
 *  6.removeGroupAttrRelation —— 解绑属性与分组
 * SQL 全部在 resources/mapper/GoodsAttrMapper.xml(参照 UserMapper.xml 的写法)。
 */
public interface GoodsAttrMapper
     extends BaseMapper<GoodsAttr> {

    /* 属性分页列表(attrName 模糊; categoryId/attrType/attrGroupId 精确; 空值跳过) */
    Page<GoodsAttrBO> getGoodsAttrList(
        Page page,
        @Param("param") GoodsAttrSearch attrSearch);

    /* 按属性 id 批量查可选值(一次 IN 查询, 避免逐行查询的 N+1) */
    List<GoodsAttrValue> getAttrValByAttrId(
        @Param("ids") List<Integer> ids);

    /* 写一条属性可选值: attr_id 取 VO.id, spu_id 固定 NULL(表示"可选值定义") */
    int addGoodsAttrVal(GoodsAttrVO attrVO);

    /* 删除某属性的全部可选值定义(只删 spu_id IS NULL 的行, 不碰已绑定 SPU 的值) */
    int removeGoodsAttrVal(
        @Param("attrId") Integer attrId);

    /* 绑定属性到分组(tbl_group_attr_relation 中间表) */
    int addGroupAttrRelation(
        @Param("attrGroupId") Integer attrGroupId,
        @Param("attrId") Integer attrId);

    /* 解除某属性的分组绑定(删除关系表中该 attr_id 的全部行) */
    int removeGroupAttrRelation(
        @Param("attrId") Integer attrId);

    int countSpuValues(@Param("attrId") Integer attrId);

    int countSkuValues(@Param("attrId") Integer attrId);

}

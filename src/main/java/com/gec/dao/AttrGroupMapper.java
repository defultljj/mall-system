package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.entity.GoodsAttrGroup;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/*
 * 属性分组 Mapper: 纯单表操作, 全部由 BaseMapper 提供 ——
 * 条件(按 categoryId 过滤 / sort 升序)在 AttrGroupServiceImpl
 * 里用 LambdaQueryWrapper 拼装, 与品牌/类别模块写法一致。
 */
public interface AttrGroupMapper
        extends BaseMapper<GoodsAttrGroup> {

    @Select("SELECT count(1) FROM tbl_group_attr_relation WHERE attr_group_id = #{id}")
    int countAttrs(@Param("id") Integer id);
}

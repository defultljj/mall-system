package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.vo.OptionVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/*
 * 下拉选项 Mapper(通用小工具, 查出来都是 {value, label} 结构):
 *  - groupByCategory: 按商品类别查属性分组(给商品管理模块用)
 *  - roleOptions:     角色下拉(排除 id=1 的系统管理员, 不允许普通编辑界面分配)
 *  - brandOptions:    品牌下拉(按类别, 商品上架第 1 步用; 只列已关联该类的品牌)
 * [清理] 资料文件 import 了 com.gec.domain.entity.GoodsAttrGroup 但方法体并未使用,
 *        实体类 GoodsAttrGroup 已按 tbl_goods_attr_group 表结构单独补齐落盘, 这里删掉多余 import。
 */
public interface OptionMapper
    extends BaseMapper<OptionVO> {

    @Select("SELECT id value, group_name label "+
            " FROM tbl_goods_attr_group "+
            " WHERE category_id=#{categoryId}" )
    List<OptionVO> groupByCategory(
        @Param("categoryId") Integer categoryId);

    @Select("SELECT id value, role_name label "+
            " FROM tbl_role "+
            " WHERE id not in(1)")
    List<OptionVO> roleOptions();

    /* 商品上架第 1 步"品牌"下拉: 只列该类别已关联的品牌.
     * 数据源直接用中间表(已有冗余 brand_name, 不必回主表 join):
     * value=brand_id, label=brand_name */
    @Select("SELECT bc.brand_id value, bc.brand_name label "+
            " FROM tbl_brand_category bc "+
            " WHERE bc.category_id=#{categoryId}")
    List<OptionVO> brandOptions(
        @Param("categoryId") Integer categoryId);

}
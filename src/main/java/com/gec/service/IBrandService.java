package com.gec.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.domain.entity.Brand;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.search.BrandSearch;
import com.gec.domain.vo.BrandCategoryVO;
import com.gec.domain.vo.BrandVO;

import java.util.List;

/*
 * 品牌服务接口。
 * 关联相关的四个方法(listBrand 的 categoryId 过滤 / getListByCategory /
 * associateCategory / getAssociatedCategories)都是本轮基于 tbl_brand_category
 * 中间表新增的"品牌 ↔ 类别"能力, 其余为品牌本体 CRUD。
 */
public interface IBrandService extends IService<Brand> {

    /** 品牌分页列表: brandName 模糊 + id 精确; categoryId 有值 → 只查已关联该类别的品牌 */
    IPage<Brand> listBrand(Page page, BrandSearch param);

    /** 按类别列出品牌(两段式打 isAssociate 标记: 1=已关联该类别, 无 categoryId 全 0) */
    IPage<BrandVO> getListByCategory(Page page, BrandSearch param);

    /** 设置显示状态(只更新 show_status 一列) */
    void setStatus(Integer id, Integer showStatus);

    /** 新增品牌 */
    void addBrand(Brand brand);

    /** 修改品牌(按 id 更新) */
    void updateBrand(Brand brand);

    /** 删除品牌(先清 tbl_brand_category 关联, 防孤儿) */
    void deleteBrand(Integer id);

    /** 按 id 查品牌详情 */
    Brand getBrand(Integer id);

    /** 品牌关联类别: 撤销 deleteIds + 差集批量插 categories(防中间表唯一键冲突) */
    void associateCategory(BrandCategoryVO bcVO);

    /** 某品牌已关联的类别列表(编辑回显) */
    List<GoodsCategory> getAssociatedCategories(Integer brandId);
}

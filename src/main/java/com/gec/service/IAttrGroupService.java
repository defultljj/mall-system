package com.gec.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.domain.entity.GoodsAttrGroup;

/*
 * 属性分组服务接口(全新模块, 实体用已有 GoodsAttrGroup)。
 * 列表按类别过滤 + sort 升序; 增/改只做简单落库(名称必填校验放 Controller),
 * 删除直接复用 IService 自带 removeById。
 */
public interface IAttrGroupService
        extends IService<GoodsAttrGroup> {

    /** 属性分组分页列表(categoryId 为空 = 全部类别), 按 sort 升序 */
    IPage<GoodsAttrGroup> listAttrGroup(Page page, Integer categoryId);

    /** 新增属性分组(id 自增回填) */
    void addAttrGroup(GoodsAttrGroup attrGroup);

    /** 修改属性分组(按 id 更新非空字段) */
    void updateAttrGroup(GoodsAttrGroup attrGroup);

    /** 删除未被属性使用的分组 */
    void deleteAttrGroup(Integer id);
}

package com.gec.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.domain.bo.GoodsAttrBO;
import com.gec.domain.entity.GoodsAttr;
import com.gec.domain.search.GoodsAttrSearch;
import com.gec.domain.vo.GoodsAttrVO;

import java.util.List;

/*
 * 规格属性服务接口(9.11 资料, 5 个方法):
 *  1.listGoodsAttr           属性分页列表(联表查分组 + 回填可选值)
 *  2.addGoodsAttr            新增属性(主表 + 可选值 + 分组关系)
 *  3.updateGoodsAttr         修改属性(子表先删后插)
 *  4.deleteGoodsAttr         删除属性(级联清子表)
 *  5.queryGoodsAttrByCategory 发布商品专用: 按类别 + 属性类型查属性
 */
public interface IGoodsAttrService
        extends IService<GoodsAttr> {

    IPage<GoodsAttrBO> listGoodsAttr(
            Page page, GoodsAttrSearch attrSearch);
    void addGoodsAttr(GoodsAttrVO goodsAttr);
    void updateGoodsAttr(GoodsAttrVO goodsAttr);
    void deleteGoodsAttr(Integer id);

    /* 【发布商品专用方法】 */
    List<GoodsAttr> queryGoodsAttrByCategory(
         Integer categoryId, Integer attrType );

}

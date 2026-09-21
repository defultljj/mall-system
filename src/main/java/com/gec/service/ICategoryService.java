package com.gec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.domain.bo.CategoryBO;
import com.gec.domain.entity.Brand;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.entity.Node;
import com.gec.domain.vo.CategoryBrandVO;

import java.util.List;

/*
 * 商品类别服务接口(树形, 参照 IDeptService)。
 * 核心都在"血缘"上: p_ids 的生成、父级变更时的级联重算、防环。
 * 本轮新增(镜像品牌侧能力):
 *  - getPids / getPidsArr: 前端面包屑/级联回显要的祖先 id 链
 *  - associateBrand: 类别关联品牌(差集批量插, 复用 tbl_brand_category)
 *  - listBrandsOfCategory: 某类别已关联的品牌列表(回显)
 */
public interface ICategoryService extends IService<GoodsCategory> {

    /** 类别树: 平铺数据转树形 List<Node>(根为顶层类别, 带 level/hasSub/children) */
    List<Node> listCategory();

    /** 新增类别(自动生成 p_ids; parentId 缺省/0 = 顶层) */
    void addCategory(GoodsCategory category);

    /** 修改类别(父级变更时, 整条分支 p_ids 级联重算) */
    void updateCategory(GoodsCategory category);

    /** 删除类别(还有子类别时拒绝; 删除前清该类别在 tbl_brand_category 的关联) */
    void deleteCategory(Integer id);

    /** 按 id 查类别详情(转成 CategoryBO 返回) */
    CategoryBO getCategory(Integer id);

    /** 获取类别 p_ids 祖先链原始串(如 "0,1,10") */
    String getPids(Integer id);

    /** 获取"祖先 + 自身" id 数组: p_ids 去掉 0, 末尾追加自身 id
     *  例: p_ids="0,1,10" id=38 → [1,10,38] */
    Integer[] getPidsArr(Integer id);

    /** 类别关联品牌(镜像 BrandController.associateCategory): 撤销 deleteIds + 差集批量插 */
    void associateBrand(CategoryBrandVO cbVO);

    /** 某类别已关联的品牌列表(编辑回显) */
    List<Brand> listBrandsOfCategory(Integer categoryId);
}

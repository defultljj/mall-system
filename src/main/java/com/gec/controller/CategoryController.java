package com.gec.controller;

import com.gec.components.FileTemplate;
import com.gec.domain.bo.CategoryBO;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.entity.Node;
import com.gec.domain.vo.CategoryBrandVO;
import com.gec.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/*
 * 商品类别管理(树形, 参照 DeptController; 本轮新增与品牌的关联 + p_ids 读接口):
 *   list             GET  /Category/list                  类别树(根为顶层类别, 带 children/level/hasSub)
 *   save             POST /Category/save                  新增类别(parentId 缺省/0 = 顶层; showStatus 默认 1)
 *   update           PUT  /Category/update                修改类别(改父类别时整条分支 p_ids 级联重算)
 *   get              GET  /Category/get?id=               按 id 查详情
 *   delete           DELETE /Category/delete?id=          删除类别(还有子类别会拒绝; 先清关联)
 *   associateBrand   POST /Category/associateBrand (JSON body)   类别 ↔ 品牌勾选保存
 *   associatedBrands GET  /Category/associatedBrands?categoryId=  某类别已关联的品牌(回显)
 *   getPids          GET  /Category/getPids?id=           p_ids 祖先链原始串(如 "0,1,10")
 *   getPidsArr       GET  /Category/getPidsArr?id=        [祖先…, 自身] id 数组(面包屑/级联回显)
 * @ExceptionHandler 与 DeptController 同款: 把 R.err 直接写回响应。
 */
@RestController
@RequestMapping("/Category")
public class CategoryController extends BaseController {
    //1.自动装配-CategoryService
    @Autowired
    private ICategoryService categoryService;

    /* 1.类别树.
     * GET /Category/list → 树形 List<Node>(无子类别时 children 不输出) */
    @GetMapping("/list")
    public R list() {
        List<Node> tree = categoryService.listCategory();
        return R.ok( tree );
    }

    /* 2.新增类别.
     * POST /Category/save (表单/查询参数: categoryName/parentId/sort/icons/
     *                     productUnit/productCount/showStatus)
     * parentId 为空或 0 → 建顶层类别; p_ids 由 service 自动生成。 */
    @PostMapping("/save")
    public R save(GoodsCategory category) {
        //1.表单没传时的默认值: 顶层(0) + 启用(1), 语义对前端更友好
        if (category.getParentId() == null) {
            category.setParentId(0);
        }
        if (category.getShowStatus() == null) {
            category.setShowStatus(1);
        }
        //2.交给 service 校验父类别存在与否并生成 p_ids
        categoryService.addCategory(category);
        return R.ok();
    }

    /* 3.修改类别.
     * PUT /Category/update (必须带 id; 改动 parentId 时 service 会级联更新子孙分支) */
    @PutMapping("/update")
    public R update(GoodsCategory category) {
        if (category.getId() == null) {
            throw new RuntimeException("缺少类别 id, 无法更新");
        }
        categoryService.updateCategory(category);
        return R.ok();
    }

    /* 4.获取类别.
     * GET /Category/get?id=1 */
    @GetMapping("/get")
    public R get(Integer id) {
        return R.ok( categoryService.getCategory(id) );
    }

    /* 5.删除类别-DELETE请求.
     * DELETE /Category/delete?id=1 (类别下还有子类别时 service 抛异常拒绝;
     * 校验通过后 service 先清 tbl_brand_category 该类别关联, 再删本体) */
    @DeleteMapping("/delete")
    public R delete(Integer id) {
        if (id == null) {
            throw new RuntimeException("缺少类别 id, 无法删除");
        }
        categoryService.deleteCategory(id);
        return R.ok();
    }

    /* 6.类别 ↔ 品牌 关联(勾选保存, 镜像品牌页).
     * POST /Category/associateBrand
     * JSON body 结构见 CategoryBrandVO: {categoryId, brands:[{id,brandName}], deleteIds:[...]}
     * 批次语义: 撤销 deleteIds 中已勾掉的关联, 差集补插 brands —— 由 service 完成 */
    @PostMapping("/associateBrand")
    public R associateBrand(@RequestBody CategoryBrandVO cbVO) {
        categoryService.associateBrand(cbVO);
        return R.ok();
    }

    /* 7.某类别已关联的品牌列表(编辑回显: 前端据此回打勾).
     * GET /Category/associatedBrands?categoryId=1 → List<Brand>(id/brandName) */
    @GetMapping("/associatedBrands")
    public R associatedBrands(Integer categoryId) {
        return R.ok( categoryService.listBrandsOfCategory(categoryId) );
    }

    /* 8.获取类别 p_ids 祖先链原始串.
     * GET /Category/getPids?id=38 → "0,1,10"(顶层类别是 "0") */
    @GetMapping("/getPids")
    public R getPids(Integer id) {
        return R.ok( categoryService.getPids(id) );
    }

    /* 9.获取"祖先 + 自身"的 id 数组(面包屑/级联回显用).
     * GET /Category/getPidsArr?id=38, p_ids="0,1,10" → [1,10,38]
     * (0 是顶层占位符会被剔除, 末尾追加自身 id) */
    @GetMapping("/getPidsArr")
    public R getPidsArr(Integer id) {
        return R.ok( categoryService.getPidsArr(id) );
    }

    /*
     * 异常处理(与 DeptController 同款): 打印堆栈后把 R.err 以 JSON 形式写回,
     * 这样 deleteCategory/associateBrand 里抛出的业务校验异常能直接以
     * {result:failed, status:500, cause:提示} 返回给前端。
     */
    @ExceptionHandler
    public void exceptionHandler(
            Exception e, HttpServletResponse resp){
        e.printStackTrace();
        R.err( e ).write( resp );
    }

    @Override
    protected FileTemplate getFileTemplate() {
        return null;
    }
}

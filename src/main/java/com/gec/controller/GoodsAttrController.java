package com.gec.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gec.components.FileTemplate;
import com.gec.domain.bo.GoodsAttrBO;
import com.gec.domain.entity.GoodsAttr;
import com.gec.domain.search.GoodsAttrSearch;
import com.gec.domain.vo.GoodsAttrVO;
import com.gec.service.IGoodsAttrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/*
 * 规格参数管理(9.11 资料: 骨架 + 补全 5 个接口):
 *   list           GET    /GoodsAttr/list?page=&limit=&attrName=&categoryId=&attrType=&attrGroupId=
 *                          属性分页列表(attrName 模糊, 其余精确, 空条件自动跳过)
 *   save           POST   /GoodsAttr/save      新增属性(名称必填; 可选值/分组关系随存)
 *   update         PUT    /GoodsAttr/update    修改属性(id 必填; 可选值/分组关系先删后插)
 *   delete         DELETE /GoodsAttr/delete?id= 删除属性(级联清理可选值/分组关系)
 *   listByCategory GET    /GoodsAttr/listByCategory/{categoryId}/{attrType}
 *                          发布商品专用: 按类别 + 属性类型(1 基本属性 | 2 销售属性)取属性列表
 * 路由已被 WebConfig 登录拦截器覆盖(/GoodsAttr/** 需登录)。
 */
@RestController
@RequestMapping("/GoodsAttr")
public class GoodsAttrController extends BaseController {
    @Autowired
    private IGoodsAttrService goodsAttrService;

    /* 【1】属性分页列表。
     * 查询条件由 Spring MVC 自动绑定到 GoodsAttrSearch(属性名/类别/类型/分组)。 */
    @GetMapping("/list")
    public R list(Integer page, Integer limit, GoodsAttrSearch attrSearch) {
        //newPage: page/limit 非法时回退默认 1/10; convertPage: 统一输出 {data, total}
        IPage<GoodsAttrBO> ip = goodsAttrService.listGoodsAttr(
                newPage(page, limit), attrSearch);
        return R.convertPage(ip);
    }

    /* 【2】新增属性(表单参数)。 */
    @PostMapping("/save")
    public R save(GoodsAttrVO attrVO) {
        //属性名称必填(列表与发布页都按名称展示)
        if (attrVO.getAttrName() == null
                || attrVO.getAttrName().trim().isEmpty()) {
            throw new RuntimeException("属性名称不能为空");
        }
        goodsAttrService.addGoodsAttr(attrVO);
        return R.ok();
    }

    /* 【3】修改属性(必须带 id; 只更新提交的非空字段)。 */
    @PutMapping("/update")
    public R update(GoodsAttrVO attrVO) {
        if (attrVO.getId() == null) {
            throw new RuntimeException("缺少属性 id, 无法更新");
        }
        if (attrVO.getAttrName() == null
                || attrVO.getAttrName().trim().isEmpty()) {
            throw new RuntimeException("属性名称不能为空");
        }
        goodsAttrService.updateGoodsAttr(attrVO);
        return R.ok();
    }

    /* 【4】删除属性(主表 + 可选值 + 分组关系 一起清)。 */
    @DeleteMapping("/delete")
    public R delete(Integer id) {
        if (id == null) {
            throw new RuntimeException("缺少属性 id, 无法删除");
        }
        goodsAttrService.deleteGoodsAttr(id);
        return R.ok();
    }

    /* 【5】listByCategory()
     * 此方法可用于查询【基本属性】与【销售属性】。
     * 传入 ==> 1.类别ID, 2.属性类型。
     * 路径参数风格: GET /GoodsAttr/listByCategory/11/1 */
    @GetMapping("/listByCategory/{categoryId}/{attrType}")
    public R listByCategory(
            @PathVariable("categoryId") Integer categoryId,
            @PathVariable("attrType") Integer attrType) {
        List<GoodsAttr> list =
                goodsAttrService.queryGoodsAttrByCategory(categoryId, attrType);
        return R.ok(list);
    }

    /* 统一异常处理(与 DeptController 同一写法): 业务错误以 R 形式写回 */
    @ExceptionHandler(Exception.class)
    public void exceptionFallback(
        Exception E, HttpServletResponse resp){
        E.printStackTrace();
        R.err( E ).write( resp );
    }

    @Override
    protected FileTemplate getFileTemplate() {
        return null;
    }
}

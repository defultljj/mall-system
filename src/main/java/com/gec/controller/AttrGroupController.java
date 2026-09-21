package com.gec.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gec.components.FileTemplate;
import com.gec.dao.OptionMapper;
import com.gec.domain.entity.GoodsAttrGroup;
import com.gec.service.IAttrGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/*
 * 商品属性分组管理(全新模块, 实体用已有 GoodsAttrGroup; 参照 RoleController 风格):
 *   list             GET    /AttrGroup/list?page=&limit=&categoryId=   分组分页列表(categoryId 可空=全部)
 *   save             POST   /AttrGroup/save                             新增分组(groupName 必填)
 *   update           PUT    /AttrGroup/update                           修改分组(必须带 id; groupName 必填)
 *   delete           DELETE /AttrGroup/delete?id=                       删除分组
 *   optionsByCategory GET   /AttrGroup/optionsByCategory?categoryId=    某类别下的分组下拉(复用 OptionMapper)
 * 所有接口统一返回 R; 业务错误 @ExceptionHandler 同款处理。
 */
@RestController
@RequestMapping("/AttrGroup")
public class AttrGroupController extends BaseController {
    //分组 CRUD 服务
    @Autowired
    private IAttrGroupService attrGroupService;
    //下拉选项 Mapper(查 {value,label} 给前端下拉框)
    @Autowired
    private OptionMapper optionMapper;

    /* 1.属性分组分页列表.
     * GET /AttrGroup/list?page=1&limit=10&categoryId=11
     * categoryId 不传/为空 = 查全部分组(商品模块给类别绑定分组时传它) */
    @GetMapping("/list")
    public R list(Integer page, Integer limit, Integer categoryId) {
        //分页查询(page/limit 非法时由 newPage 回退默认 1/10), 列表按 sort 升序
        IPage<GoodsAttrGroup> ip = attrGroupService.listAttrGroup(
                newPage(page, limit), categoryId);
        return R.convertPage(ip);
    }

    /* 2.新增属性分组.
     * POST /AttrGroup/save (表单/查询参数: groupName/categoryId/descript/icons/sort) */
    @PostMapping("/save")
    public R save(GoodsAttrGroup attrGroup) {
        //分组名称必填(商品属性列表页按组名展示)
        if (attrGroup.getGroupName() == null
                || attrGroup.getGroupName().trim().isEmpty()) {
            throw new RuntimeException("分组名称不能为空");
        }
        attrGroupService.addAttrGroup(attrGroup);
        return R.ok();
    }

    /* 3.修改属性分组.
     * PUT /AttrGroup/update (必须带 id; 只更新提交的非空字段) */
    @PutMapping("/update")
    public R update(GoodsAttrGroup attrGroup) {
        if (attrGroup.getId() == null) {
            throw new RuntimeException("缺少分组 id, 无法更新");
        }
        if (attrGroup.getGroupName() == null
                || attrGroup.getGroupName().trim().isEmpty()) {
            throw new RuntimeException("分组名称不能为空");
        }
        attrGroupService.updateAttrGroup(attrGroup);
        return R.ok();
    }

    /* 4.删除属性分组.
     * DELETE /AttrGroup/delete?id=1 (属性明细表尚未开发, 当前无级联清理) */
    @DeleteMapping("/delete")
    public R delete(Integer id) {
        if (id == null) {
            throw new RuntimeException("缺少分组 id, 无法删除");
        }
        attrGroupService.deleteAttrGroup(id);
        return R.ok();
    }

    /* 5.按类别查属性分组下拉(商品属性模块"选分组"时用).
     * GET /AttrGroup/optionsByCategory?categoryId=11
     * → List<OptionVO>({value:分组id, label:分组名}), 复用 OptionMapper.groupByCategory */
    @GetMapping("/optionsByCategory")
    public R optionsByCategory(Integer categoryId) {
        return R.ok( optionMapper.groupByCategory(categoryId) );
    }

    /* 统一异常处理(与 DeptController 同一写法): 业务错误以 R 形式写回 */
    @ExceptionHandler
    public void exceptionHandler(Exception e, HttpServletResponse resp) {
        e.printStackTrace();
        R.err(e).write(resp);
    }

    @Override
    protected FileTemplate getFileTemplate() {
        return null;
    }
}

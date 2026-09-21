package com.gec.controller;

import com.gec.components.FileTemplate;
import com.gec.domain.entity.Dept;
import com.gec.domain.entity.Node;
import com.gec.service.IDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/*
 * 部门管理:
 *   list   GET    /Dept/list           部门树(根为顶层部门, 带 children/level/hasSub)
 *   save   POST   /Dept/save           新增部门(parentId 缺省/0 = 顶层)
 *   update PUT    /Dept/update         修改部门(改父部门时整条分支 p_ids 级联重算)
 *   get    GET    /Dept/get?id=        按 id 查详情
 *   delete DELETE /Dept/delete?id=     删除部门(有人/有子部门会拒绝)
 * @ExceptionHandler 异常处理保留自资料并保持原样: 把 R.err 直接写回响应。
 */
@RestController
@RequestMapping("/Dept")
public class DeptController
    extends BaseController {
    //1.自动装配-DeptService
    @Autowired
    private IDeptService deptService;

    /* 1.部门列表.
     * GET /Dept/list → 树形 List<Node>(无子部门时 children 不输出) */
    @GetMapping("/list")
    public R list() {
        List<Node> tree = deptService.listDept();
        return R.ok( tree );
    }

	/* 2.添加部门.
	 * POST /Dept/save (表单/查询参数: deptName/deptDesc/parentId) */
    @PostMapping("/save")
    public R save(Dept dept) {
        //父部门为空或 0 表示建顶层部门; p_ids 由 service 自动生成
        deptService.addDept(dept);
        return R.ok();
    }

    /* 3.更新部门.
     * PUT /Dept/update (必须带 id; 改动 parentId 时 service 会级联更新子孙分支) */
    @PutMapping("/update")
    public R update(Dept dept) {
        deptService.updateDept(dept);
        return R.ok();
    }

    /* 4.获取部门.
     * GET /Dept/get?id=1 */
    @GetMapping("/get")
    public R get(Integer id) {
        return R.ok( deptService.getDept(id) );
    }

    /* 5.删除部门-DELETE请求.
     * DELETE /Dept/delete?id=1 (部门下还有用户/子部门时 service 抛异常拒绝) */
    @DeleteMapping("/delete")
    public R delete(Integer id) {
        deptService.deleteDept(id);
        return R.ok();
    }

    /*
     * 异常处理(资料原样保留): 打印堆栈后把 R.err 以 JSON 形式写回,
     * 这样 deleteDept/addDept 里抛出的业务校验异常能直接以
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
package com.gec.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gec.components.FileTemplate;
import com.gec.dao.OptionMapper;
import com.gec.domain.entity.Role;
import com.gec.domain.vo.OptionVO;
import com.gec.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 角色管理:
 *   roleOptions GET    /Role/roleOptions   角色下拉(排除 id=1 系统管理员) [资料已有]
 *   list        GET    /Role/list          角色分页列表
 *   save        POST   /Role/save          新增角色
 *   update      PUT    /Role/update        修改角色
 *   get         GET    /Role/get?id=       按 id 查详情
 *   delete      DELETE /Role/delete?id=    删除角色
 * 角色 CRUD 是资料的待补空壳, 此处按与 User/Dept 模块一致的风格补齐。
 */
@RestController
@RequestMapping("/Role")
public class RoleController extends BaseController {
    //1.自动装配 UserService 接口
    @Autowired
    private OptionMapper optionMapper;
    //角色 CRUD 服务
    @Autowired
    private IRoleService roleService;
    @Autowired
    private JdbcTemplate jdbc;

    /*
    * Link:  /Role/roleOptions
    * 角色下拉: 排除 id=1(系统管理员), 不允许普通界面把它分配给别人
    */
    @GetMapping("/roleOptions")
    public R roleOptions(){
        List<OptionVO> ops = optionMapper.roleOptions();
        return R.ok( ops );
    }

    /* 角色分页列表 GET /Role/list?page=1&limit=10&roleName=xx */
    @GetMapping("/list")
    public R list(Integer page, Integer limit, String roleName) {
        Map<String,Object> data = new HashMap<>();
        data.put("roleName", roleName);
        IPage<Role> ip = roleService.listRole(newPage(page, limit), data);
        return R.convertPage(ip);
    }

    /* 新增角色 POST /Role/save */
    @PostMapping("/save")
    public R save(Role role) {
        roleService.addRole(role);
        return R.ok().put("id", role.getId());
    }

    /* 修改角色 PUT /Role/update (必须带 id) */
    @PutMapping("/update")
    public R update(Role role) {
        roleService.updateRole(role);
        return R.ok();
    }

    /* 按 id 查角色详情 GET /Role/get?id=1 */
    @GetMapping("/get")
    public R get(Integer id) {
        return R.ok( roleService.getRole(id) );
    }

    @GetMapping("/permissions")
    public R permissions(Integer id) {
        return R.ok(jdbc.queryForList("SELECT p.id,p.permission,p.path,EXISTS(SELECT 1 FROM tbl_role_permission rp WHERE rp.role_id=? AND rp.permission_id=p.id) selected FROM tbl_permission p ORDER BY p.id", id == null ? -1 : id));
    }

    @PostMapping("/savePermissions")
    @Transactional(rollbackFor = Exception.class)
    public R savePermissions(Integer roleId, String permissionIds) {
        if (roleId == null) throw new RuntimeException("角色编号不能为空");
        jdbc.update("DELETE FROM tbl_role_permission WHERE role_id=?", roleId);
        if (permissionIds != null && !permissionIds.trim().isEmpty()) {
            for (String token : permissionIds.split(",")) {
                jdbc.update("INSERT INTO tbl_role_permission(role_id,permission_id) VALUES(?,?)", roleId, Integer.valueOf(token.trim()));
            }
        }
        return R.ok();
    }

    /* 删除角色 DELETE /Role/delete?id=1 */
    @DeleteMapping("/delete")
    public R delete(Integer id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    /* 统一异常处理: 业务错误以 R 形式写回(与 DeptController 同一写法) */
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

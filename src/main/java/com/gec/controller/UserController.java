package com.gec.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gec.components.FileTemplate;
import com.gec.domain.bo.UserBO;
import com.gec.domain.entity.User;
import com.gec.domain.vo.UserSearchVO;
import com.gec.domain.vo.LoginUserVO;
import com.gec.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/*
 * 用户管理(资料里只剩注释空壳, 以下按注释声明的请求动词补齐):
 *   list   GET    /User/list     分页联表列表(部门名/角色名), 支持搜索条件
 *   save   POST   /User/save     新增用户 + 分配角色
 *   update PUT    /User/update   修改用户 + 替换角色关联
 *   get    GET    /User/get?id=  按 id 查详情
 *   delete DELETE /User/delete?id= 删除(连带清 user_role 关联)
 * 所有接口统一返回 R(键: result/status/data/total/cause)。
 */
@RestController
@RequestMapping("/User")
public class UserController extends BaseController {
    /* 1.自动装配 UserService 接口. */
    @Autowired
    private IUserService userService;

	/* 2.用户列表 list().
	 * GET /User/list?page=1&limit=10&account=xx&nickName=xx&phone=xx&deptId=xx
	 * (search 参数对象由 Spring 按查询参数自动封装进 UserSearchVO) */
    @GetMapping("/list")
    public R list(Integer page, Integer limit, UserSearchVO search) {
        //1.搜索条件 VO → Map(UserMapper.getList 只认 Map 参数)
        Map<String,Object> param = new HashMap<>();
        if (search != null) {
            param.put("account",  search.getAccount());
            param.put("nickName", search.getNickName());
            param.put("phone",    search.getPhone());
            param.put("deptId",   search.getDeptId());
        }
        //2.分页联表查询(带部门名/角色名; page/limit 非法时由 newPage 回退默认 1/10)
        IPage<UserBO> ip = userService.listUser(newPage(page, limit), param);
        return R.convertPage(ip);
    }

    /* 3.添加用户-POST请求.
     * POST /User/save  (表单/查询参数; roleId 为该用户分配的角色) */
    @PostMapping("/save")
    public R save(User user, Integer roleId) {
        //user.id 为空 → 走新增, 内部同时写入 tbl_user_role 关联
        userService.saveUser(user, roleId);
        return R.ok();
    }

    /* 4.更新用户-PUT请求.
     * PUT /User/update (表单/查询参数; 必须带 id, roleId 缺省则清空该用户的角色) */
    @PutMapping("/update")
    public R update(User user, Integer roleId) {
        //复用 saveUser: 有 id 即更新; 内部"先删旧角色关联, 再加新关联"完成替换
        userService.saveUser(user, roleId);
        return R.ok();
    }

    /* 5.获取用户-GET请求.
     * GET /User/get?id=1 */
    @GetMapping("/get")
    public R get(Integer id) {
        return R.ok( userService.getUser(id) );
    }

    /* 6.删除用户-DELETE请求.
     * DELETE /User/delete?id=1 (同时清理 tbl_user_role 关联) */
    @DeleteMapping("/delete")
    public R delete(Integer id, HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser instanceof LoginUserVO
                && id != null && id.equals(((LoginUserVO) loginUser).getId())) {
            throw new RuntimeException("不能删除当前登录账号");
        }
        userService.deleteUser(id);
        return R.ok();
    }

    @PostMapping("/setStatus")
    public R setStatus(Integer id, Integer status, HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (status != null && status == 0 && loginUser instanceof LoginUserVO
                && id != null && id.equals(((LoginUserVO) loginUser).getId())) {
            throw new RuntimeException("不能停用当前登录账号");
        }
        userService.setStatus(id, status);
        return R.ok();
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

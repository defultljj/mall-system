package com.gec.controller;

import com.gec.components.FileTemplate;
import com.gec.dao.UserMapper;
import com.gec.domain.entity.User;
import com.gec.domain.vo.LoginAccountVO;
import com.gec.domain.vo.LoginUserVO;
import com.gec.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/*
 * 登录 / 注册 / 登出 / 会话检查(拦截器白名单, 匿名可访问):
 *   login    POST   /Auth/login       账号密码登录(bcrypt 比对)
 *   register POST   /Auth/register    注册新账号(默认分配角色 id=5 部门职员)
 *   logout   POST   /Auth/logout      登出(销毁 session)
 *   check    GET    /Auth/check       查当前登录态(前端据此决定显示登录页还是主界面)
 * 登录态: session["loginUser"] = LoginUserVO —— 全程不含 password。
 * WebConfig 里拦截器只注册了业务路径(/User/** 等), /Auth/** 不在其中,
 * 所以本 Controller 四个接口天然匿名可访问, 无需额外放行配置。
 */
@RestController
@RequestMapping("/Auth")
public class AuthController extends BaseController {
    //1.登录/注册都直接查 tbl_user(查重与比对共用 findByAccount)
    @Autowired
    private UserMapper userMapper;
    //2.注册时要写"用户-角色"关联, 复用角色模块的能力(参照 UserServiceImpl 的用法)
    @Autowired
    private IRoleService roleService;
    //3.bcrypt 加密器(spring-security-crypto 提供的纯工具类, 不进过滤器链, 线程安全可复用)
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /* 注册时默认分配的角色: id=5 部门职员(与 tbl_role 现有数据一致) */
    private static final int DEFAULT_ROLE_ID = 5;

    /* ==================== 登录 ==================== */
    /*
     * POST /Auth/login?account=xx&password=xx
     * 1.findByAccount 查"含密码行"(账号不存在查出来是 null);
     * 2.bcrypt 比对明文与库中哈希 —— 账号或密码任一不对都只提示"账号或密码错误",
     *   不告诉攻击者到底是哪个错了;
     * 3.成功 → 联表查登录信息 VO → 存 session → 返回前端。
     */
    @PostMapping("/login")
    public R login(String account, String password, HttpSession session) {
        //1.查"含密码行"(LoginAccountVO 是内部载体, 只做比对用, 不返回前端)
        LoginAccountVO acct = userMapper.findByAccount(account);
        //2.账号不存在 / 没传密码 / 明文与哈希不匹配 → 统一提示。
        //   (先短路 null/空密码: BCrypt 对 null 明文内部会 NPE;
        //    库中密码为 NULL 时 matches 会记警告并返回 false, 同样进这个分支)
        if (acct == null || password == null || password.isEmpty()
                || !encoder.matches(password, acct.getPassword())) {
            throw new RuntimeException("账号或密码错误");
        }
        if (Integer.valueOf(0).equals(acct.getStatus())) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }
        //3.联表取登录信息(带部门名/角色名)
        LoginUserVO vo = userMapper.findLoginUser(acct.getId());
        //4.登录态写入 session(AuthInterceptor 检查的就是这个键)
        session.setAttribute("loginUser", vo);
        return R.ok(vo);
    }

    /* ==================== 注册 ==================== */
    /*
     * POST /Auth/register?account&nickName&password&phone&sex&email&deptId(可选)
     * 步骤: 账号非空 + 查重 → 密码 bcrypt 加密 → 插入用户 → 回填 id 写密码列
     *       → 默认分配角色 id=5(部门职员)。
     * 多步写库包在 @Transactional 里(参照 UserServiceImpl.saveUser 的事务用法),
     * 任一步失败整体回滚, 不会留下"有用户没角色/有角色没密码"的半截数据。
     */
    @PostMapping("/register")
    @Transactional(rollbackFor = Exception.class)
    public R register(String account, String nickName, String password,
            String phone, String sex, String email, Integer deptId) {
        //1.账号非空校验(登录名是唯一身份标识, 不允许空值)
        if (account == null || account.trim().isEmpty()) {
            throw new RuntimeException("账号不能为空");
        }
        //2.账号查重 —— 与登录共用同一个 findByAccount
        if (userMapper.findByAccount(account) != null) {
            throw new RuntimeException("账号已存在");
        }
        //3.密码非空校验(密码列虽可空, 但没有密码的账号将永远无法登录)
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        //4.密码 bcrypt 加密后入库(绝不存明文)
        String hash = encoder.encode(password);

        //5.写用户资料(User 实体不含 password 列, 密码单独 UPDATE 进 tbl_user)
        User user = new User();
        user.setAccount(account.trim());
        user.setNickName(nickName);
        user.setPhone(phone);
        user.setSex(sex);
        user.setEmail(email);
        user.setDeptId(deptId);
        user.setStatus(1);
        userMapper.insert(user);   //自增 id 回填到 user.id
        Integer userId = user.getId();
        if (userId == null) {
            throw new RuntimeException("注册失败, 未取得用户 id");
        }
        //6.密码哈希写入密码列(单独 UPDATE: User 实体刻意没有该字段)
        userMapper.updatePassword(userId, hash);
        //7.默认分配角色 id=5 部门职员(参照 UserServiceImpl 里 roleService 的用法)
        roleService.addUserRoleAssociation(userId, DEFAULT_ROLE_ID);
        return R.ok();
    }

    /* ==================== 登出 ==================== */
    /* POST /Auth/logout: 销毁整个 session, 登录态即失效(拦截器下一次就拦) */
    @PostMapping("/logout")
    public R logout(HttpSession session) {
        session.invalidate();
        return R.ok();
    }

    /* ==================== 会话检查 ==================== */
    /*
     * GET /Auth/check:
     * 有登录态 → R.ok(LoginUserVO); 没有 → R.ok(null)。
     * 前端启动时先调它: data 有值进主界面, 为 null 跳登录页。
     */
    @GetMapping("/check")
    public R check(HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        return R.ok(loginUser);
    }

    /*
     * 统一异常处理(与 DeptController 同一写法):
     * 拦截器只管会话; 这里处理的都是业务校验异常
     * ("账号或密码错误" / "账号已存在" / "账号不能为空" ...),
     * 以 {result:failed, status:500, cause:提示} 写回前端。
     */
    @ExceptionHandler
    public void exceptionHandler(
            Exception e, HttpServletResponse resp) {
        e.printStackTrace();
        R.err(e).write(resp);
    }

    @Override
    protected FileTemplate getFileTemplate() {
        return null;
    }
}

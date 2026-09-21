package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gec.domain.bo.UserBO;
import com.gec.domain.entity.User;
import com.gec.domain.vo.LoginAccountVO;
import com.gec.domain.vo.LoginUserVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

/*
 * 用户 Mapper:
 *  - BaseMapper<User>: MyBatis-Plus 提供的单表 CRUD
 *  - getList():       联表分页查询(部门名/角色名), SQL 实现在 mapper/UserMapper.xml
 *                     分页由 MyBatis-Plus 分页插件完成(第一个参数传 Page 即可)
 *  - findByAccount / updatePassword: 登录注册模块专用 —— tbl_user.password 不在
 *                     User 实体上(防止密码字段被 JSON 序列化返回前端),
 *                     所以"查含密码行 / 单独写密码列"都走这里显式声明的注解 SQL。
 *  - findLoginUser(): 登录成功后联表组装 LoginUserVO(部门名/角色名)。
 */
public interface UserMapper extends BaseMapper<User> {

    Page<UserBO> getList(
       Page page,
       @Param("param") Map<String, Object> param );

    /*
     * 按账号查"含密码行"(注册查重与登录比对共用):
     * 结果映射进 LoginAccountVO, 由 MyBatis 全局 map-underscore-to-camel-case
     * 自动把 nick_name → nickName / dept_id → deptId; 该 VO 只做服务端内部载体。
     */
    @Select("SELECT * FROM tbl_user WHERE account = #{account}")
    LoginAccountVO findByAccount(@Param("account") String account);

    /*
     * 按 id 联表查登录信息: tbl_user + tbl_dept + tbl_user_role + tbl_role。
     * 一人一个角色的场景(多角色时同 id 会出多行, 本系统按单角色设计)。
     * 列别名直接写成 LoginUserVO 的驼峰属性, 与 mapper/UserMapper.xml 的 getList 同款写法。
     */
    @Select(" SELECT u.id        AS id,       " +
            "        u.account   AS account,  " +
            "        u.nick_name AS nickName, " +
            "        u.dept_id   AS deptId,   " +
            "        d.dept_name AS deptName, " +
            "        r.id        AS roleId,   " +
            "        r.role_name AS roleName  " +
            "   FROM tbl_user u               " +
            "   LEFT JOIN tbl_dept d          " +
            "     ON d.id = u.dept_id         " +
            "   LEFT JOIN tbl_user_role u_role ON u_role.user_id = u.id " +
            "   LEFT JOIN tbl_role r          " +
            "     ON r.id = u_role.role_id    " +
            "  WHERE u.id = #{id}             ")
    LoginUserVO findLoginUser(@Param("id") Integer id);

    /*
     * 单独写密码列(bcrypt 哈希): User 实体没有 password 字段,
     * MyBatis-Plus 的 updateById 不会触碰该列, 必须用 SQL 显式更新。
     */
    @Update("UPDATE tbl_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(
            @Param("id") Integer id,
            @Param("password") String password);

}
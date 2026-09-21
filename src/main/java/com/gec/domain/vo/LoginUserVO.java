package com.gec.domain.vo;

import lombok.Data;

/*
 * 登录用户 VO: /Auth/login 成功后返回前端, 并整体存入 session(键名 "loginUser")。
 *  - 字段来自 tbl_user + tbl_dept(部门名) + tbl_role(角色), 一人一个角色的业务场景。
 *  - 绝不含 password: 登录态只携带前端展示/权限判断需要的信息。
 */
@Data
public class LoginUserVO {
    private Integer id;        //用户 id
    private String account;    //账号
    private String nickName;   //昵称
    private Integer deptId;    //所属部门 id
    private String deptName;   //部门名(联表)
    private Integer roleId;    //角色 id(联表)
    private String roleName;   //角色名(联表)
}

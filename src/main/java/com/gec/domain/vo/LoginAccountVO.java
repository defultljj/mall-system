package com.gec.domain.vo;

import lombok.Data;

/*
 * 登录账号 VO(仅作 Mapper 查询结果的"含密码载体", 不返回前端):
 *  - User 实体刻意不带 password 字段(防止资料字段被 JSON 序列化泄漏),
 *    而登录校验必须要拿到密码哈希才能做 bcrypt 比对,
 *    所以由 UserMapper.findByAccount 用本 VO 承接 tbl_user 的完整行。
 *  - 使用场景: /Auth/register 账号查重 + /Auth/login 密码比对, 查询后立即丢弃。
 */
@Data
public class LoginAccountVO {
    private Integer id;       //用户 id
    private String account;   //账号
    private String password;  //bcrypt 密码哈希(仅在服务端内部使用)
    private String nickName;  //昵称
    private Integer deptId;   //所属部门 id
    private Integer status;   //1=启用 0=禁用
}

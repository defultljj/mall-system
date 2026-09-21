package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/*
 * 用户实体(资料原样, 无 password 字段):
 *  - tbl_user.password 列建表为 "varchar(255) DEFAULT NULL"(可空, 无默认值),
 *    所以新增用户时不给密码在数据库层面没有任何问题。
 *  - 表中已有数据的 password 是 bcrypt 哈希($2a$10$...), 但资料 pom 无 security 依赖,
 *    故本项目不引入 bcrypt —— 密码属于登录安全模块的职责(外部模块负责写入),
 *    这个后台管理模块只负责用户资料的维护, 不触碰密码列。
 */
@Data
@TableName("tbl_user")
public class User extends BaseEntity {
    @TableId(type=IdType.AUTO)
    private Integer id;
    private String account;
    private String nickName;
    private String phone;
    private String sex;
    private String no;
    private String email;
    private Integer deptId;
    private Integer status;

}

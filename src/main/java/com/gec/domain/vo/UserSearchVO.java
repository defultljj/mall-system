package com.gec.domain.vo;

import lombok.Data;

/*
 * 用户列表搜索条件 VO(资料里是空壳类, 字段按 UserController 列表查询的实际需要补齐)。
 * GET /User/list?account=xx&nickName=xx&phone=xx&deptId=xx
 * Controller 收到后用 BeanUtils/手动方式转成 Map 传给 UserMapper.getList 的 param。
 */
@Data
public class UserSearchVO {
    private String account;     //账号(模糊查询)
    private String nickName;    //姓名(模糊查询)
    private String phone;       //手机号(模糊查询)
    private Integer deptId;     //部门 id(精确查询)
}
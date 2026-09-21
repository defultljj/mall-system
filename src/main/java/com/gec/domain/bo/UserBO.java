package com.gec.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/*
 * 用户 BO: 列表页要展示的数据载体。
 * 相比 User 实体多了联表查出的 部门名/角色id/角色名。
 * (UserMapper.xml 的 getList 查询结果直接映射成这个类)
 */
@Data
public class UserBO {
    private Integer id;
    private String account;
    private String nickName;
    private String phone;
    private String sex;
    private String no;
    private Integer deptId;
    private String deptName;
    private Integer roleId;
    private String roleName;
    private Integer status;

    @JsonFormat(pattern="yyyy-MM-dd")
    private Date createDate;
}

package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 角色实体 → 对应 tbl_role 表。
 * [Bug 修复] 资料里 @TableName 误写成 "tbl_dept"(从部门实体复制粘贴的笔误),
 *            会直接操作错表, 这里改正为 "tbl_role"。
 */
@Data
@TableName(value="tbl_role")
@EqualsAndHashCode(callSuper = false)
public class Role extends BaseEntity {
    @TableId(type= IdType.AUTO)
    private Integer id;
    private String roleName;
    private String descript;

}
package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Date;

/*
 * 部门实体 → 对应 tbl_dept 表。
 * 实现 Node 接口, 因此可以放进部门树。
 * p_ids: 上代所有部门的 id 链(如 "0,1,2"), 用于快速判断血缘关系。
 */
@Data
@TableName(value="tbl_dept")
@EqualsAndHashCode(callSuper = false)
public class Dept extends BaseEntity implements Node {
    @TableId(type=IdType.AUTO)
    private Integer id;
    private String deptName;     //dept_name
    private String deptDesc;     //dept_desc
    private Integer parentId;    //parent_id

    @TableField(value="p_ids")
    private String pIds;         //p_ids

}
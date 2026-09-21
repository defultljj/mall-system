package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.entity.Dept;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/*
 * 部门 Mapper。
 * getUserCount / getSubDeptCount 在资料里只有签名没有 SQL,
 * 这里用 @Select 注解补齐实现(供 DeptServiceImpl.deleteDept 删除前做校验)。
 */
public interface DeptMapper extends BaseMapper<Dept> {

    //{1}获取某部门的用户数量. (where 前要有空格)
    @Select(" SELECT count(1) FROM tbl_user WHERE dept_id = #{deptId} ")
    int getUserCount(@Param("deptId") Integer deptId);

    //{2}获取子部门的数量 (某个部门下有几个 "一级" 下属部门)
    //   (where 前要有空格)
    @Select(" SELECT count(1) FROM tbl_dept WHERE parent_id = #{deptId} ")
    int getSubDeptCount(@Param("deptId") Integer deptId);

    //List<Dept> getList();
}
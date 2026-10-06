package com.gec.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gec.domain.entity.Role;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/*
 * 角色 Mapper:
 * 除 BaseMapper 的单表 CRUD 外, 还需要维护"用户-角色"关联表 tbl_user_role,
 * 对应 IRoleService.addUserRoleAssociation / removeUserRoleAssociation 的调用。
 */
public interface RoleMapper extends BaseMapper<Role> {

    /* 1.添加用户-角色的关联项。 */
    @Insert("INSERT INTO tbl_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int addUserRoleAssociation(
            @Param("userId") Integer userId,
            @Param("roleId") Integer roleId);

    /* 2.删除用户-角色的关联项(一个用户的所有角色)。 */
    @Delete("DELETE FROM tbl_user_role WHERE user_id = #{userId}")
    int removeUserRoleAssociation(@Param("userId") Integer userId);

}
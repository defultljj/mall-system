package com.gec.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.domain.entity.Role;

import java.util.Map;

public interface IRoleService extends IService<Role> {

    /** 给用户新增一条角色关联(写 tbl_user_role) */
    void addUserRoleAssociation(
            Integer userId, Integer roleId);

    /** 删除某用户的全部角色关联 */
    void removeUserRoleAssociation(
            Integer userId);

    /** 角色分页查询(data 可带 roleName 模糊条件) */
    IPage<Role> listRole(
            Page page, Map<String, Object> data);

    /** 删除角色 */
    void deleteRole(Integer id);

    /** 新增角色 */
    void addRole(Role role);

    /** 修改角色 */
    void updateRole(Role role);

    /** 按 id 查角色详情 */
    Role getRole(Integer id);

}
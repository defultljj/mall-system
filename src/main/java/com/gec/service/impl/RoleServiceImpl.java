package com.gec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.dao.RoleMapper;
import com.gec.domain.entity.Role;
import com.gec.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RoleServiceImpl
    extends ServiceImpl<RoleMapper, Role>
    implements IRoleService {
    @Autowired
    private RoleMapper roleMapper;

    /* 新增一条"用户-角色"关联 → 写 tbl_user_role */
    @Override
    public void addUserRoleAssociation(
        Integer userId, Integer roleId) {
		/* --填入代码(1)-- */
        roleMapper.addUserRoleAssociation(userId, roleId);
    }

    /* 删除某用户的全部角色关联 */
    @Override
    public void removeUserRoleAssociation(Integer userId) {
		/* --填入代码(2)-- */
        roleMapper.removeUserRoleAssociation(userId);
    }

    /* 角色分页列表: data 里可带 roleName 做模糊过滤 */
    @Override
    public IPage<Role> listRole(
        Page page, Map<String, Object> data) {
        QueryWrapper<Role> qw = new QueryWrapper<>();
        if (data != null && data.get("roleName") != null
                && !"".equals(data.get("roleName").toString().trim())) {
            qw.like("role_name", data.get("roleName").toString().trim());
        }
        qw.orderByAsc("id");   //列表顺序稳定
        return roleMapper.selectPage(page, qw);
    }

    /* 删除角色 */
    @Override
    public void deleteRole(Integer id) {
        roleMapper.deleteById(id);
    }

    /* 新增角色(id 自增回填) */
    @Override
    public void addRole(Role role) {
        roleMapper.insert(role);
    }

    /* 修改角色(按 id 更新非空字段) */
    @Override
    public void updateRole(Role role) {
        roleMapper.updateById(role);
    }

    /* 按 id 查详情 */
    @Override
    public Role getRole(Integer id) {
        return roleMapper.selectById(id);
    }
}
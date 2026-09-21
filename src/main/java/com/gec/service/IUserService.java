package com.gec.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gec.domain.bo.UserBO;
import com.gec.domain.entity.User;

import java.util.Map;

public interface IUserService extends IService<User> {

    /** 分页联表查询用户(含部门名/角色名), param 为搜索条件 Map */
    IPage<UserBO> listUser(
            Page page, Map<String, Object> data);

    /** 保存/更新用户, 并维护用户-角色关联(先删旧关联, 再加新关联) */
    void saveUser(User user, Integer roleId);

    /** 按 id 查用户详情 */
    User getUser(Integer id);

    /** 删除用户(同时清理 tbl_user_role 关联) */
    void deleteUser(Integer id);

    /** 启用或禁用用户 */
    void setStatus(Integer id, Integer status);
}

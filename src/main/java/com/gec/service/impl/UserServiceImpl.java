package com.gec.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gec.dao.UserMapper;
import com.gec.domain.bo.UserBO;
import com.gec.domain.entity.User;
import com.gec.service.IRoleService;
import com.gec.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UserServiceImpl
    extends ServiceImpl<UserMapper,User>
    implements IUserService {
    /*
    *  以下提示注入错误, 可以这样处理。
    *  ALT + ENTER
    */
    @Autowired
    private UserMapper userMapper;
    @Autowired                        //(打开注释)
    private IRoleService roleService; //(打开注释)

    /* 分页联表查询: SQL 在 mapper/UserMapper.xml 的 getList(返回带部门名/角色名的 UserBO) */
    @Override
    public IPage<UserBO> listUser(
        Page page, Map<String, Object> param ) {
		/*-- 请填入代码 --*/
        return userMapper.getList(page, param);
    }

    /*
     * 保存/更新用户 + 维护用户-角色关联。
     * 注释意图: 编辑用户时角色可能被改掉, 所以统一"先删旧关联、再插入新关联"。
     * (id 为空 → 新增并回填自增 id; id 有值 → 更新)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUser(User user, Integer roleId) {
        boolean ret = saveOrUpdate(user);
        /*-- 请填入代码 --*/
        //1.取出用户 id(新增时 MyBatis-Plus 会把自增主键回填进来)
        Integer userId = user.getId();
        if (userId == null) {
            throw new RuntimeException("用户保存失败, 未取得用户 id");
        }
        //2.先清掉旧的角色关联(兼容编辑场景的"替换关联")
        roleService.removeUserRoleAssociation(userId);
        //3.再写入新关联(roleId 为空表示暂不分配角色)
        if (roleId != null && roleId.intValue() > 0) {
            roleService.addUserRoleAssociation(userId, roleId);
        }
    }

    @Override
    public User getUser(Integer id) {
        return userMapper.selectById(id);
    }

    /* 删除用户: 先删 tbl_user_role 关联, 再删用户本身, 不留孤儿数据 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Integer id) {
        if (id == null) {
            throw new RuntimeException("用户参数不正确");
        }
        if (id == 1) {
            throw new RuntimeException("系统管理员账号不能删除");
        }
        if (userMapper.selectById(id) == null) {
            throw new RuntimeException("用户不存在");
        }
        roleService.removeUserRoleAssociation(id);
        userMapper.deleteById(id);
    }

    @Override
    public void setStatus(Integer id, Integer status) {
        if (id == null || status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("用户状态参数不正确");
        }
        if (id == 1 && status == 0) {
            throw new RuntimeException("系统管理员账号不能停用");
        }
        LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<>();
        update.eq(User::getId, id).set(User::getStatus, status);
        if (userMapper.update(null, update) == 0) {
            throw new RuntimeException("用户不存在");
        }
    }
}

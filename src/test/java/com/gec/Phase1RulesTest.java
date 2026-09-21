package com.gec;

import com.gec.dao.AttrGroupMapper;
import com.gec.dao.CategoryMapper;
import com.gec.dao.GoodsAttrMapper;
import com.gec.dao.UserMapper;
import com.gec.domain.entity.GoodsAttrGroup;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.vo.GoodsAttrVO;
import com.gec.service.IRoleService;
import com.gec.service.impl.AttrGroupServiceImpl;
import com.gec.service.impl.GoodsAttrServiceImpl;
import com.gec.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase1RulesTest {

    @Mock private CategoryMapper categoryMapper;
    @Mock private AttrGroupMapper attrGroupMapper;
    @Mock private GoodsAttrMapper goodsAttrMapper;
    @Mock private UserMapper userMapper;
    @Mock private IRoleService roleService;

    @InjectMocks private AttrGroupServiceImpl attrGroupService;
    @InjectMocks private GoodsAttrServiceImpl goodsAttrService;
    @InjectMocks private UserServiceImpl userService;

    @Test
    void attributeGroupRejectsLeafThatIsNotThirdLevel() {
        GoodsCategory secondLevelLeaf = category(10, "0,1");
        when(categoryMapper.selectById(10)).thenReturn(secondLevelLeaf);

        GoodsAttrGroup group = new GoodsAttrGroup();
        group.setCategoryId(10);
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> attrGroupService.addAttrGroup(group));

        assertEquals("属性分组只能绑定第三级叶子类别", error.getMessage());
        verify(attrGroupMapper, never()).insert(group);
    }

    @Test
    void goodsAttributeRejectsLeafThatIsNotThirdLevel() {
        GoodsCategory secondLevelLeaf = category(10, "0,1");
        when(categoryMapper.selectById(10)).thenReturn(secondLevelLeaf);

        GoodsAttrVO attr = new GoodsAttrVO();
        attr.setAttrName("测试属性");
        attr.setCategoryId(10);
        attr.setAttrType(1);
        attr.setValueType(1);
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> goodsAttrService.addGoodsAttr(attr));

        assertEquals("商品属性只能绑定第三级叶子类别", error.getMessage());
        verify(goodsAttrMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void systemAdministratorCannotBeDeleted() {
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> userService.deleteUser(1));

        assertEquals("系统管理员账号不能删除", error.getMessage());
        verify(roleService, never()).removeUserRoleAssociation(1);
        verify(userMapper, never()).deleteById(1);
    }

    private GoodsCategory category(Integer id, String pIds) {
        GoodsCategory category = new GoodsCategory();
        category.setId(id);
        category.setPIds(pIds);
        return category;
    }
}

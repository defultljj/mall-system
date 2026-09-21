package com.gec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.dao.AttrGroupMapper;
import com.gec.dao.CategoryMapper;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.entity.GoodsAttrGroup;
import com.gec.service.IAttrGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
 * 属性分组服务实现(全新模块):
 *  - listAttrGroup: categoryId 为空查全部, 有值按类别过滤; 统一 sort 升序(数值小在前);
 *  - 增/改: 直接透传 BaseMapper(与品牌模块 addBrand/updateBrand 同款写法);
 *  - 删除: 复用 ServiceImpl 自带的 removeById, 不重复封装。
 */
@Service
public class AttrGroupServiceImpl
        extends ServiceImpl<AttrGroupMapper, GoodsAttrGroup>
        implements IAttrGroupService {
    @Autowired
    private AttrGroupMapper attrGroupMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    /* 属性分组分页列表: categoryId 可空(空 = 全部类别); sort 升序, 同序再按 id 升序 */
    @Override
    public IPage<GoodsAttrGroup> listAttrGroup(Page page, Integer categoryId) {
        LambdaQueryWrapper<GoodsAttrGroup> qw = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            qw.eq(GoodsAttrGroup::getCategoryId, categoryId);   //按类别过滤
        }
        qw.orderByAsc(GoodsAttrGroup::getSort)   //sort 升序(数值小在前)
          .orderByAsc(GoodsAttrGroup::getId);    //同序按 id 升序, 列表稳定
        return attrGroupMapper.selectPage(page, qw);
    }

    /* 新增属性分组(id 自增回填; create/update 时间由元数据处理器自动填充) */
    @Override
    public void addAttrGroup(GoodsAttrGroup attrGroup) {
        validateCategory(attrGroup.getCategoryId());
        attrGroupMapper.insert(attrGroup);
    }

    /* 修改属性分组(按 id 更新非空字段) */
    @Override
    public void updateAttrGroup(GoodsAttrGroup attrGroup) {
        GoodsAttrGroup old = attrGroupMapper.selectById(attrGroup.getId());
        if (old == null) {
            throw new RuntimeException("属性分组不存在");
        }
        attrGroup.setCategoryId(old.getCategoryId());
        attrGroupMapper.updateById(attrGroup);
    }

    @Override
    public void deleteAttrGroup(Integer id) {
        if (attrGroupMapper.countAttrs(id) > 0) {
            throw new RuntimeException("该分组下还有属性，不能删除");
        }
        attrGroupMapper.deleteById(id);
    }

    private void validateCategory(Integer categoryId) {
        GoodsCategory category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("请选择有效的所属类别");
        }
        if (!isThirdLevel(category) || categoryMapper.countSub(categoryId) > 0) {
            throw new RuntimeException("属性分组只能绑定第三级叶子类别");
        }
    }

    private boolean isThirdLevel(GoodsCategory category) {
        String pIds = category.getPIds();
        return pIds != null && pIds.split(",").length == 3;
    }
}

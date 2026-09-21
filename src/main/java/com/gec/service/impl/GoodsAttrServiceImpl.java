package com.gec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.dao.GoodsAttrMapper;
import com.gec.dao.AttrGroupMapper;
import com.gec.dao.CategoryMapper;
import com.gec.domain.bo.GoodsAttrBO;
import com.gec.domain.entity.GoodsAttr;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.entity.GoodsAttrValue;
import com.gec.domain.search.GoodsAttrSearch;
import com.gec.domain.vo.GoodsAttrVO;
import com.gec.service.IGoodsAttrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/*
 * 规格属性服务实现(9.11 资料, 补全 5 处留白)。
 *
 * 子表语义(与 XML 一致): tbl_goods_attr_value 里 spu_id IS NULL 的行 = 属性的"可选值定义",
 * 本模块只维护这些行; tbl_group_attr_relation 维护 属性↔分组 的绑定。
 * 增/改/删三个写方法都加了 @Transactional: 主表与子表必须同生共死。
 */
@Service
public class GoodsAttrServiceImpl
    extends ServiceImpl<GoodsAttrMapper, GoodsAttr>
    implements IGoodsAttrService {
    @Autowired
    private GoodsAttrMapper goodsAttrMapper;
    @Autowired
    private AttrGroupMapper attrGroupMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    /* 【A】属性分页列表: 联表分页 + 批量回填可选值 */
    @Override
    public IPage<GoodsAttrBO> listGoodsAttr(
            Page page, GoodsAttrSearch attrSearch) {
        //1.主查询: 属性 + 分组关系 + 分组名(动态条件在 XML 里)
        IPage<GoodsAttrBO> ip = goodsAttrMapper.getGoodsAttrList(page, attrSearch);
        List<GoodsAttrBO> boList = ip.getRecords();
        if (boList == null || boList.isEmpty()) {
            return ip;
        }
        //2.收集本页所有属性 id, 一次 IN 查询取回可选值(避免每行再查一次的 N+1)
        List<Integer> ids = boList.stream()
                .map(GoodsAttrBO::getId)
                .collect(Collectors.toList());
        List<GoodsAttrValue> valList = goodsAttrMapper.getAttrValByAttrId(ids);
        //3.按 attrId 分组, 方便回填
        Map<Integer, List<GoodsAttrValue>> valMap = valList.stream()
                .collect(Collectors.groupingBy(GoodsAttrValue::getAttrId));
        //4.逐行回填:
        //   单行 → 走 setAttrValue(GoodsAttrValue) 重载, 顺带反填 valueType/valueTypeName;
        //   多行 → 用 ';' 拼成一个字符串, 走 setAttrValue(String) 重载(与多值型存储格式一致)。
        for (GoodsAttrBO attrBO : boList) {
            List<GoodsAttrValue> vals = valMap.get(attrBO.getId());
            if (vals == null || vals.isEmpty()) {
                continue;   //该属性没有可选值, attr_value 主表值原样展示
            }
            if (vals.size() == 1) {
                attrBO.setAttrValue(vals.get(0));
            } else {
                String joined = vals.stream()
                        .map(GoodsAttrValue::getAttrValue)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(";"));
                attrBO.setAttrValue(joined);
            }
        }
        return ip;
    }

    /* 【B】新增属性: 主表(回填 id) → 可选值 → 分组关系 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGoodsAttr(GoodsAttrVO attrVO) {
        validate(attrVO, null);
        //1.VO → 实体, 落主表; 自增 id 会回填到 entity
        GoodsAttr entity = getEntity(attrVO);
        goodsAttrMapper.insert(entity);
        //2.把回填的自增 id 同步回 VO: 值表/关系表都以它为外键
        attrVO.setId( entity.getId() );
        //3.可选值: attrValue 非空才写值表(spu_id=NULL, 表示"可选值定义")
        if (attrVO.getAttrValue()!=null && !attrVO.getAttrValue().trim().isEmpty()) {
            goodsAttrMapper.addGoodsAttrVal(attrVO);
        }
        //4.分组关系: attrGroupId 非空才绑定
        if (attrVO.getAttrGroupId()!=null) {
            goodsAttrMapper.addGroupAttrRelation(attrVO.getAttrGroupId(), entity.getId());
        }
    }

    /* 【C】修改属性: 主表更新 + 子表先删后插(保证与提交内容完全一致) */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGoodsAttr(GoodsAttrVO attrVO) {
        GoodsAttr old = goodsAttrMapper.selectById(attrVO.getId());
        if (old == null) {
            throw new RuntimeException("商品属性不存在");
        }
        validate(attrVO, old);
        attrVO.setCategoryId(old.getCategoryId());
        attrVO.setAttrType(old.getAttrType());
        //1.更新主表非空字段(updateById 只更新非空属性)
        GoodsAttr entity = getEntity(attrVO);
        goodsAttrMapper.updateById(entity);
        //2.可选值: 先删旧定义, 再按本次提交重建
        goodsAttrMapper.removeGoodsAttrVal( attrVO.getId() );
        if (attrVO.getAttrValue()!=null && !attrVO.getAttrValue().trim().isEmpty()) {
            goodsAttrMapper.addGoodsAttrVal(attrVO);
        }
        //3.分组关系: 同样先删后插(一个属性只属于一个分组)
        goodsAttrMapper.removeGroupAttrRelation( attrVO.getId() );
        if (attrVO.getAttrGroupId()!=null) {
            goodsAttrMapper.addGroupAttrRelation(attrVO.getAttrGroupId(), attrVO.getId());
        }
    }

    /* 【D】删除属性: 主表 + 可选值 + 分组关系 一起清 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGoodsAttr(Integer id) {
        if (goodsAttrMapper.countSpuValues(id) > 0
                || goodsAttrMapper.countSkuValues(id) > 0) {
            throw new RuntimeException("该属性已被商品或 SKU 使用，不能删除；可改为停用");
        }
        goodsAttrMapper.removeGoodsAttrVal(id);        //可选值定义行
        goodsAttrMapper.removeGroupAttrRelation(id);   //分组关系
        goodsAttrMapper.deleteById(id);                //主表最后删，兼容外键约束
    }

    /* 【E】发布商品专用: 按类别 + 属性类型查属性(发布商品第 2/3 步选规格用) */
    @Override
    public List<GoodsAttr> queryGoodsAttrByCategory(Integer categoryId, Integer attrType) {
        QueryWrapper<GoodsAttr> qw = new QueryWrapper<>();
        qw.eq("category_id", categoryId);
        qw.eq("attr_type", attrType);
        qw.eq("enable", 1);
        qw.orderByAsc("id");
        return goodsAttrMapper.selectList(qw);
    }

    /* VO → 实体(资料的私有转换方法, 原样保留) */
    private GoodsAttr getEntity(GoodsAttrVO attrVO) {
        GoodsAttr attr = new GoodsAttr();
        attr.setId( attrVO.getId() );
        attr.setAttrName( attrVO.getAttrName() );
        attr.setCategoryId( attrVO.getCategoryId() );
        attr.setAttrType( attrVO.getAttrType() );
        attr.setValueType( attrVO.getValueType() );
        attr.setAttrValue( attrVO.getAttrValue() );
        attr.setEnable( attrVO.getEnable() );
        attr.setSearchEnable( attrVO.getSearchEnable() );
        return attr;
    }

    private void validate(GoodsAttrVO attrVO, GoodsAttr old) {
        if (attrVO == null || attrVO.getAttrName() == null
                || attrVO.getAttrName().trim().isEmpty()) {
            throw new RuntimeException("属性名称不能为空");
        }
        attrVO.setAttrName(attrVO.getAttrName().trim());
        Integer categoryId = old == null ? attrVO.getCategoryId() : old.getCategoryId();
        Integer attrType = old == null ? attrVO.getAttrType() : old.getAttrType();
        GoodsCategory category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("请选择有效的所属类别");
        }
        String pIds = category.getPIds();
        if (pIds == null || pIds.split(",").length != 3
                || categoryMapper.countSub(categoryId) > 0) {
            throw new RuntimeException("商品属性只能绑定第三级叶子类别");
        }
        if (attrType == null || (attrType != 1 && attrType != 2)) {
            throw new RuntimeException("属性类型只能是规格参数或销售属性");
        }
        if (attrVO.getValueType() == null
                || (attrVO.getValueType() != 1 && attrVO.getValueType() != 2)) {
            throw new RuntimeException("值类型只能是单值或多值");
        }
        if (attrVO.getAttrGroupId() != null) {
            com.gec.domain.entity.GoodsAttrGroup group =
                    attrGroupMapper.selectById(attrVO.getAttrGroupId());
            if (group == null || !categoryId.equals(group.getCategoryId())) {
                throw new RuntimeException("属性分组必须属于当前类别");
            }
        }
        if (attrVO.getEnable() == null) attrVO.setEnable(1);
        if (attrVO.getSearchEnable() == null) attrVO.setSearchEnable(0);
    }
}

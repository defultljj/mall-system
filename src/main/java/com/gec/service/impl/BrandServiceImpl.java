package com.gec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.dao.BrandMapper;
import com.gec.dao.CategoryMapper;
import com.gec.domain.entity.Brand;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.search.BrandSearch;
import com.gec.domain.vo.BrandCategoryVO;
import com.gec.domain.vo.BrandVO;
import com.gec.service.IBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * 品牌服务实现。
 *  - 品牌本体 CRUD 沿用 LambdaQueryWrapper / updateById 的既有风格;
 *  - 关联中间表 tbl_brand_category 的"删/插"都在本类完成:
 *    删除先清关联、批量插先算差集(唯一键 uni_brand_category 保证一对关联只有一行),
 *    冗余名称列(brand_name/category_name)一律取各自主表真实值, 不信任前端提交;
 *  - 跨表写(品牌 + 中间表)统一加 @Transactional。
 * 关联表不在实体映射里, 关联操作全部走 BrandMapper 的注解 SQL, 不走 XML。
 */
@Service
public class BrandServiceImpl
        extends ServiceImpl<BrandMapper, Brand>
        implements IBrandService {
    @Autowired
    private BrandMapper brandMapper;
    //跨模块复用类别表的只读能力(查真实 category_name / selectBatchIds)
    @Autowired
    private CategoryMapper categoryMapper;

    /* 品牌分页列表: brandName 模糊、id 精确;
     * categoryId 有值时用子查询过滤出"已关联该类别"的品牌(单条 SQL, 不走 XML) */
    @Override
    public IPage<Brand> listBrand(Page page, BrandSearch param) {
        LambdaQueryWrapper<Brand> qw = new LambdaQueryWrapper<>();
        if (param != null) {
            if (param.getId() != null) {
                qw.eq(Brand::getId, param.getId());   //id 精确
            }
            String name = param.getBrandName();
            if (name != null && !"".equals(name.trim())) {
                qw.like(Brand::getBrandName, name.trim());   //名称模糊
            }
            if (param.getCategoryId() != null) {
                //类别有值 → 只留已关联该类别的品牌(子查询扫中间表)
                qw.inSql(Brand::getId, "SELECT brand_id FROM tbl_brand_category"
                        + " WHERE category_id=" + param.getCategoryId());
            }
        }
        qw.orderByAsc(Brand::getId);   //按 id 升序, 列表顺序稳定
        return brandMapper.selectPage(page, qw);
    }

    /* 按类别列出品牌(带 isAssociate 标记), 教学约定的"两段式":
     * 第一步先普通分页查品牌(可按名称模糊);
     * 第二步 categoryId 有值时, 用"当前页品牌 id"批量查中间表已关联集合,
     *      命中的品牌 isAssociate 置 1, 其余(含 categoryId 为空)一律 0。
     * 只对当前页做关联查询, 不跨页扫描 —— 与分页天然契合。 */
    @Override
    public IPage<BrandVO> getListByCategory(Page page, BrandSearch param) {
        //1.两段式第一步: 分页查品牌(可带名称模糊, 空值自动跳过)
        LambdaQueryWrapper<Brand> qw = new LambdaQueryWrapper<>();
        if (param != null) {
            String name = param.getBrandName();
            if (name != null && !"".equals(name.trim())) {
                qw.like(Brand::getBrandName, name.trim());
            }
        }
        qw.orderByAsc(Brand::getId);
        IPage<Brand> brandPage = brandMapper.selectPage(page, qw);
        List<Brand> records = brandPage.getRecords();

        //2.品牌实体 → VO, isAssociate 先全部默认 0
        List<BrandVO> voList = new ArrayList<>();
        for (Brand b : records) {
            voList.add(toBrandVO(b));
        }

        //3.两段式第二步: categoryId 有值 → 只对"当前页"打标记
        if (param != null && param.getCategoryId() != null && !voList.isEmpty()) {
            List<Integer> pageBrandIds = new ArrayList<>();
            for (Brand b : records) {
                pageBrandIds.add(b.getId());
            }
            Set<Integer> associated = new HashSet<>(
                    brandMapper.selectBrandIdsOfCategory(param.getCategoryId(), pageBrandIds));
            for (BrandVO vo : voList) {
                if (associated.contains(vo.getId())) {
                    vo.setIsAssociate(1);   //已关联该类别的品牌置 1
                }
            }
        }

        //4.重建 BrandVO 分页对象(总数与筛选前一致, 前端按 total 分页)
        Page<BrandVO> result = new Page<>(
                brandPage.getCurrent(), brandPage.getSize(), brandPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    /* 设置显示状态: LambdaUpdateWrapper 只更新 show_status 一列
     * (不会把表单没提交的其它字段误更新成 null) */
    @Override
    public void setStatus(Integer id, Integer showStatus) {
        if (id == null) {
            throw new RuntimeException("缺少品牌 id, 无法设置状态");
        }
        LambdaUpdateWrapper<Brand> uw = new LambdaUpdateWrapper<>();
        uw.eq(Brand::getId, id);
        if (showStatus != null) {
            uw.set(Brand::getShowStatus, showStatus);
        }
        brandMapper.update(null, uw);
    }

    /* 新增品牌(id 自增回填; create/update 时间由元数据处理器自动填充) */
    @Override
    public void addBrand(Brand brand) {
        brandMapper.insert(brand);
    }

    /* 修改品牌(按 id 更新非空字段) */
    @Override
    public void updateBrand(Brand brand) {
        brandMapper.updateById(brand);
    }

    /* 删除品牌: 先清中间表关联, 再删品牌本体 —— 防孤儿记录
     * (tbl_brand_category 没有外键, 漏删会在商品模块按类别统计时出错) */
    @Override
    @Transactional
    public void deleteBrand(Integer id) {
        if (brandMapper.countCategoryAssociations(id) > 0) {
            throw new RuntimeException("该品牌已关联商品类别，请先解除关联");
        }
        if (brandMapper.countSpus(id) > 0) {
            throw new RuntimeException("该品牌已被商品使用，不能删除");
        }
        brandMapper.deleteById(id);
    }

    /* 按 id 查品牌详情(查不到返回 null, 由 R.ok(null) 交给前端判断) */
    @Override
    public Brand getBrand(Integer id) {
        return brandMapper.selectById(id);
    }

    /* 品牌关联类别(勾选保存):
     * 1.selectById 校验品牌存在;
     * 2.deleteIds 非空 → 撤销这些类别关联;
     * 3.categories 非空 → 与"已关联类别"做差集, 只插差集(去重, 防撞唯一键);
     * 4.冗余名取真实值: brand_name 用品牌实体, category_name 用类别表 selectBatchIds 查。
     * 跨表写(校验读 + 删 + 插)放在一个事务里。 */
    @Override
    @Transactional
    public void associateCategory(BrandCategoryVO bcVO) {
        //1.品牌必须真实存在(拿它当 brand_name 冗余列的真实值来源)
        if (bcVO == null || bcVO.getBrandId() == null) {
            throw new RuntimeException("缺少品牌 id, 无法关联类别");
        }
        Brand brand = brandMapper.selectById(bcVO.getBrandId());
        if (brand == null) {
            throw new RuntimeException("品牌(id=" + bcVO.getBrandId() + ")不存在, 无法关联类别");
        }
        //2.先撤销前端取消勾选的关联
        List<Integer> deleteIds = bcVO.getDeleteIds();
        if (deleteIds != null && !deleteIds.isEmpty()) {
            brandMapper.removeAssociate(brand.getId(), deleteIds);
        }
        //3.差集: 本次勾选 − 已关联 = 真正需要插入的(去重, 防唯一键冲突)
        List<GoodsCategory> categories = bcVO.getCategories();
        if (categories == null || categories.isEmpty()) {
            return;
        }
        Set<Integer> existed = new HashSet<>(
                brandMapper.selectCategoryIdsByBrand(brand.getId()));
        Set<Integer> diffIds = new LinkedHashSet<>();   //有序去重
        for (GoodsCategory c : categories) {
            if (c.getId() != null && !existed.contains(c.getId())) {
                diffIds.add(c.getId());
            }
        }
        if (diffIds.isEmpty()) {
            return;   //勾选的都已关联过, 无需插入
        }
        //4.冗余 category_name 列取真实值(查类别表, 不信任前端提交的名称)
        Map<Integer, GoodsCategory> catMap = new HashMap<>();
        for (GoodsCategory c : categoryMapper.selectBatchIds(diffIds)) {
            catMap.put(c.getId(), c);
        }
        List<GoodsCategory> toInsert = new ArrayList<>();
        for (Integer cid : diffIds) {
            GoodsCategory real = catMap.get(cid);
            if (real != null && real.getCategoryName() != null
                    && !"".equals(real.getCategoryName().trim())) {
                toInsert.add(real);   //实体自带 id/categoryName, 直接给 SQL 用
            }
        }
        if (toInsert.isEmpty()) {
            return;
        }
        //5.组装"只含待插数据"的 VO 再批量插
        BrandCategoryVO insertVO = new BrandCategoryVO();
        insertVO.setBrandId(brand.getId());
        insertVO.setBrandName(brand.getBrandName());   //brand_name 取品牌表真实值
        insertVO.setCategories(toInsert);
        brandMapper.associateCategory(insertVO);
    }

    /* 某品牌已关联的类别列表(编辑回显: 前端据此回打勾)
     * 联表查由 CategoryMapper.selectByBrandId 提供, 只返回 id/categoryName */
    @Override
    public List<GoodsCategory> getAssociatedCategories(Integer brandId) {
        if (brandId == null) {
            return new ArrayList<>();
        }
        return categoryMapper.selectByBrandId(brandId);
    }

    /* 品牌实体 → 品牌 VO(字段一致, 只补视图字段默认值) */
    private BrandVO toBrandVO(Brand b) {
        BrandVO vo = new BrandVO();
        vo.setId(b.getId());
        vo.setBrandName(b.getBrandName());
        vo.setLogoName(b.getLogoName());
        vo.setBrandDesc(b.getBrandDesc());
        vo.setShowStatus(b.getShowStatus());
        vo.setCreateDate(b.getCreateDate());
        vo.setUpdateDate(b.getUpdateDate());
        vo.setIsAssociate(0);   //默认 0, 命中关联再置 1
        return vo;
    }
}

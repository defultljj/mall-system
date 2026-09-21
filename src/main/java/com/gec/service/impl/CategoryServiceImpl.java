package com.gec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.dao.BrandMapper;
import com.gec.dao.CategoryMapper;
import com.gec.domain.bo.CategoryBO;
import com.gec.domain.entity.Brand;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.entity.Node;
import com.gec.domain.vo.CategoryBrandVO;
import com.gec.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * 类别服务实现: 树形核心逻辑一比一参照 DeptServiceImpl 移植。
 * 类别表与部门表同为"p_ids 血缘"树形模型(顶层 p_ids='0', 子级='父p_ids,父id'),
 * 所以建树/挂树/p_ids 生成/防环级联整套算法可以直接复用, 只需把
 * Dept → GoodsCategory、DeptBO → CategoryBO 逐一替换。
 * 本轮新增: 类别与品牌的中间表关联(associateBrand / listBrandsOfCategory),
 * 以及 p_ids 祖先链的两个读接口(getPids / getPidsArr);
 * 删除类别前会先清 tbl_brand_category 里该类别的关联(防孤儿)。
 */
@Service
public class CategoryServiceImpl
        extends ServiceImpl<CategoryMapper, GoodsCategory>
        implements ICategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    //跨模块复用品牌表的只读能力(查真实 brand_name / selectBatchIds)
    @Autowired
    private BrandMapper brandMapper;

    /* ================================================================
     * 树形处理: 把"线性类别列表" ==> "层次类别树"
     * ================================================================ */

    /*
     * 平铺类别列表 → 树:
     * 1.全部转成 CategoryBO 放入 Map(id → 节点);
     * 2.遍历时把每个节点挂到父节点(parent_id)下;
     * 3.找不到父节点(parent_id=0/父类别不在数据里)的作为根(顶层类别);
     * 4.自顶向下填充 level(根=1 递增)与 hasSub(有子节点为 true)。
     */
    private List<Node> buildTree(List<GoodsCategory> categoryList) {
        //1.按 id 升序转 BO 并放入 LinkedHashMap(保证后面的挂树顺序稳定)
        Map<Integer, CategoryBO> nodeMap = new LinkedHashMap<>();
        for (GoodsCategory c : categoryList) {
            nodeMap.put(c.getId(), new CategoryBO(c));
        }
        //2.挂树 + 收集根
        List<Node> roots = new ArrayList<>();
        for (CategoryBO node : nodeMap.values()) {
            CategoryBO parent = nodeMap.get(node.getParentId());
            if (parent != null && parent != node) {
                parent.addChildNode(node);
            } else {
                roots.add(node);
            }
        }
        //3.填充 level / hasSub
        for (Node root : roots) {
            fillLevelAndHasSub(root, 1);
        }
        return roots;
    }

    /* 递归填充: level 根=1 递增; 有子节点置 hasSub=true */
    private void fillLevelAndHasSub(Node node, int level) {
        node.setLevel(level);
        List<Node> children = (node instanceof CategoryBO)
                ? ((CategoryBO) node).getChildren() : null;
        boolean hasSub = (children != null && !children.isEmpty());
        if (hasSub) {
            for (Node child : children) {
                fillLevelAndHasSub(child, level + 1);
            }
        }
        node.setHasSub(hasSub);
    }

    /* 类别树: 平铺 → 树(前端分类下拉/维护页用的都是这个树) */
    @Override
    public List<Node> listCategory() {
        //1.查全部类别(id 升序, 树输出顺序稳定)
        List<GoodsCategory> list = categoryMapper.selectList(
                new QueryWrapper<GoodsCategory>().orderByAsc("id"));
        //2.平铺 → 树
        return buildTree(list);
    }

    /* ================================================================
     * p_ids 血缘链生成(新增 / 变更父级时共用)
     * ================================================================ */

    /*
    *  ID              父ID    PIDS(上代所有 ID)
    *  1   手机/运营商/数码  0     0
    *  10  手机通讯        1     0,1
    *  11  游戏手机        10    0,1,10   [新的PIDS]
    * p_ids 生成规则与 tbl_goods_category 已有数据完全一致:
    *   顶层类别(父=0/null): p_ids = "0"
    *   子类别:              p_ids = 父类别.p_ids + "," + 父类别.id
    * 注意: 本表有自引用外键 parent_id→id, 库里不存在 id=0,
    * 顶层类别的 parent_id 必须置 NULL 才能入库(0 只是"顶层"的入参语义)。
    */
    private GoodsCategory setParentIds(GoodsCategory c) {
        Integer parentId = c.getParentId();
        if (parentId == null || parentId <= 0) {
            //1.作为顶层类别: parent_id 置 null(满足外键), p_ids 固定 "0"
            c.setParentId(null);
            c.setPIds("0");
            return c;
        }
        //2.父类别必须真实存在(自引用外键 + 需要拿父的 p_ids 来拼接)
        GoodsCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new RuntimeException("父类别(id=" + parentId + ")不存在, 无法创建子类别");
        }
        if (levelOf(parent.getPIds()) >= 3) {
            throw new RuntimeException("商品类别最多只能创建 3 级");
        }
        //3.子类别 p_ids = 父类别 p_ids + "," + 父类别 id
        String parentPIds = parent.getPIds();
        if (parentPIds == null || parentPIds.trim().isEmpty()) {
            parentPIds = "0";   //父数据异常时兜底, 视为挂在根下
        }
        c.setPIds(parentPIds + "," + parentId);
        return c;
    }

    /* 新增类别: 校验父类别 + 生成 p_ids 后入库 */
    @Override
    public void addCategory(GoodsCategory category) {
        //1.校验父类别存在与否 + 生成 p_ids
        requireName(category);
        setParentIds(category);
        //2.入库(自增 id 由 MyBatis-Plus 回填到 category.id)
        categoryMapper.insert(category);
    }

    /* ================================================================
     * 修改: 父级变更 → 自身 p_ids 重算 + 子孙整条分支级联重算(防环)
     * ================================================================ */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(GoodsCategory category) {
        //1.被修改的类别必须存在
        GoodsCategory old = categoryMapper.selectById(category.getId());
        if (old == null) {
            throw new RuntimeException("要修改的类别不存在(id=" + category.getId() + ")");
        }
        requireName(category);
        //旧路径前缀: 直属子类别的 p_ids 恰等于它, 更深子孙以它开头
        String oldPrefix = (old.getPIds() == null ? "0" : old.getPIds())
                + "," + old.getId();

        //2.父级参数三态(前端表单语义):
        //   ① null —— 本次没提交父级 → 父级保持不变(防呆: 只改名称/排序时不会误移整棵树)
        //   ② 0   —— 显式移到顶层 → parent_id 置 null 落库, p_ids 变 "0"
        //   ③ >0  —— 移到指定父级 → 校验父存在 + 防环 + 按父的 p_ids 重算
        //   ②③ 都会改变自身 p_ids, 必须把该分支所有子孙的 p_ids 前缀级联重算
        Integer newParentId = category.getParentId();
        if (newParentId == null) {
            //① 父级未提交: 沿用库里的旧血缘(不信任前端提交的 p_ids)
            category.setParentId(old.getParentId());
            category.setPIds(old.getPIds());
        } else {
            if (newParentId > 0) {
                //3.防环: 新父类别不能是它自己, 也不能是它自己的子孙
                //   (自引用树一旦成环, 子树就会从树上"消失", 数据直接不可恢复)
                GoodsCategory newParent =
                        categoryMapper.selectById(newParentId);
                if (newParent == null) {
                    throw new RuntimeException("父类别(id=" + newParentId + ")不存在");
                }
                String np = newParent.getPIds();
                if (np != null
                        && (np.equals(oldPrefix) || np.startsWith(oldPrefix + ","))) {
                    throw new RuntimeException(
                            "不能把类别移动到它的下级类别下(会形成环)");
                }
            }
            int newLevel = newParentId == null || newParentId <= 0
                    ? 1 : levelOf(categoryMapper.selectById(newParentId).getPIds()) + 1;
            if (newLevel + subtreeHeight(old) - 1 > 3) {
                throw new RuntimeException("移动后商品类别层级将超过 3 级");
            }
            //4.重算自身 p_ids(0 → 顶层 null+"0"; >0 → 父.p_ids + "," + 父id)
            setParentIds(category);
            //5.级联: 该分支所有子孙的 p_ids 前缀 旧→新(只更新受影响的分支)
            updateSubTreePIds(oldPrefix,
                    (category.getPIds() == null ? "0" : category.getPIds())
                            + "," + category.getId());
        }
        //6.更新类别本体(parent_id 列因 updateStrategy=IGNORED 可显式写 null)
        categoryMapper.updateById(category);
    }

    /*
     * 级联更新子孙 p_ids 前缀:
     * 子孙 = p_ids 恰等于 oldPrefix 的(直属子类别) + 以 "oldPrefix," 开头的(更深层)。
     * 每个子孙去掉旧前缀, 换成 newPrefix, 后半段原样保留。
     * 例: 把 id=10(0,1) 从 id=1 下移到 id=2 下 → 新 p_ids = 父(2)的 "0,2" + ",10",
     *     它的子孙(11~16 等, p_ids 原为 "0,1,10,...") 前缀换成 "0,2,10", 后半段保留。
     */
    private void updateSubTreePIds(String oldPrefix, String newPrefix) {
        QueryWrapper<GoodsCategory> qw = new QueryWrapper<>();
        qw.eq("p_ids", oldPrefix)
                .or(w -> w.likeRight("p_ids", oldPrefix + ","));
        List<GoodsCategory> subs = categoryMapper.selectList(qw);
        for (GoodsCategory sub : subs) {
            String suffix =
                    sub.getPIds().substring(oldPrefix.length());
            sub.setPIds(newPrefix + suffix);
            categoryMapper.updateById(sub);
        }
    }

    /*
     * 删除类别:
     * tbl_goods_category 建了自引用外键(parent_id → id),
     * 直接 delete 一个"还有子类别"的行会触发外键约束报错(MySQL 拒绝),
     * 所以先在业务层用 countSub 拦住, 提示前端先删光子类别(自底向上删)。
     * 校验通过后先清 tbl_brand_category 里该类别的关联, 再删本体 —— 防孤儿。
     * 两处删除在一个事务里。
     */
    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        //1.类别下还有子类别 → 拒绝删除
        int subCount = categoryMapper.countSub(id);
        if (subCount > 0) {
            throw new RuntimeException(
                    "该类别下还有子类别, 请先删除子类别");
        }
        if (categoryMapper.countBrandAssociations(id) > 0) {
            throw new RuntimeException("该类别已关联品牌，请先解除关联");
        }
        if (categoryMapper.countAttrGroups(id) > 0 || categoryMapper.countAttrs(id) > 0) {
            throw new RuntimeException("该类别已配置属性组或商品属性，不能删除");
        }
        if (categoryMapper.countSpus(id) > 0) {
            throw new RuntimeException("该类别已被商品使用，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    /* 按 id 查详情: 实体 → BO(与树节点同构, 编辑回显字段一致) */
    @Override
    public CategoryBO getCategory(Integer id) {
        GoodsCategory c = categoryMapper.selectById(id);
        if (c == null) {
            return null;   //查不到返回 null, 由 R.ok(null) 交给前端判断
        }
        return new CategoryBO(c);
    }

    /* ================================================================
     * 本轮新增: p_ids 祖先链读接口(前端面包屑/级联回显用)
     * ================================================================ */

    /* 取类别 p_ids 原始串(如 "0,1,10"; 顶层是 "0") */
    @Override
    public String getPids(Integer id) {
        if (id == null) {
            return null;
        }
        return categoryMapper.getPids(id);
    }

    /*
     * 取"祖先 + 自身"的 id 数组:
     * p_ids 是字符串 "0,1,10", 拆出祖先时把 0(顶层占位符)去掉,
     * 末尾再追加自身 id → [1,10,38]。p_ids 为空/异常时只返回自身。
     * (VUE 端面包屑与类别回显要的就是这个 Integer[])
     */
    @Override
    public Integer[] getPidsArr(Integer id) {
        if (id == null) {
            throw new RuntimeException("缺少类别 id, 无法计算 id 序列");
        }
        String pIds = categoryMapper.getPids(id);   //如 "0,1,10"
        List<Integer> seq = new ArrayList<>();
        if (pIds != null && !"".equals(pIds.trim())) {
            for (String seg : pIds.split(",")) {
                String s = seg.trim();
                if (s.length() > 0 && !"0".equals(s)) {   //祖先链里的 0 剔除
                    try {
                        seq.add(Integer.valueOf(s));
                    } catch (NumberFormatException e) {
                        //脏数据兜底: 跳过非数字段, 不打断整个接口
                    }
                }
            }
        }
        seq.add(id);   //末尾追加自身 id
        return seq.toArray(new Integer[0]);
    }

    /* ================================================================
     * 本轮新增: 类别 ↔ 品牌关联(镜像 BrandService.associateCategory)
     * ================================================================ */

    /* 类别关联品牌(勾选保存), 与品牌侧对称:
     * 1.selectById 校验类别存在;
     * 2.deleteIds 非空 → 撤销这些品牌关联;
     * 3.brands 非空 → 与"已关联品牌"做差集, 只插差集(去重, 防撞唯一键);
     * 4.冗余名取真实值: category_name 用类别实体, brand_name 用品牌表 selectBatchIds 查。
     * 跨表写(校验读 + 删 + 插)放在一个事务里。 */
    @Override
    @Transactional
    public void associateBrand(CategoryBrandVO cbVO) {
        //1.类别必须真实存在(拿它当 category_name 冗余列的真实值来源)
        if (cbVO == null || cbVO.getCategoryId() == null) {
            throw new RuntimeException("缺少类别 id, 无法关联品牌");
        }
        GoodsCategory category = categoryMapper.selectById(cbVO.getCategoryId());
        if (category == null) {
            throw new RuntimeException("类别(id=" + cbVO.getCategoryId() + ")不存在, 无法关联品牌");
        }
        //2.先撤销前端取消勾选的关联
        List<Integer> deleteIds = cbVO.getDeleteIds();
        if (deleteIds != null && !deleteIds.isEmpty()) {
            categoryMapper.removeAssociate(category.getId(), deleteIds);
        }
        //3.差集: 本次勾选 − 已关联 = 真正需要插入的(去重, 防唯一键冲突)
        List<Brand> brands = cbVO.getBrands();
        if (brands == null || brands.isEmpty()) {
            return;
        }
        Set<Integer> existed = new HashSet<>(
                categoryMapper.selectBrandIdsByCategory(category.getId()));
        Set<Integer> diffIds = new LinkedHashSet<>();   //有序去重
        for (Brand b : brands) {
            if (b.getId() != null && !existed.contains(b.getId())) {
                diffIds.add(b.getId());
            }
        }
        if (diffIds.isEmpty()) {
            return;   //勾选的都已关联过, 无需插入
        }
        //4.冗余 brand_name 列取真实值(查品牌表, 不信任前端提交的名称)
        Map<Integer, Brand> brandMap = new HashMap<>();
        for (Brand b : brandMapper.selectBatchIds(diffIds)) {
            brandMap.put(b.getId(), b);
        }
        List<Brand> toInsert = new ArrayList<>();
        for (Integer bid : diffIds) {
            Brand real = brandMap.get(bid);
            if (real != null && real.getBrandName() != null
                    && !"".equals(real.getBrandName().trim())) {
                Brand row = new Brand();
                row.setId(real.getId());
                row.setBrandName(real.getBrandName());
                toInsert.add(row);
            }
        }
        if (toInsert.isEmpty()) {
            return;
        }
        //5.组装"只含待插数据"的 VO 再批量插
        CategoryBrandVO insertVO = new CategoryBrandVO();
        insertVO.setCategoryId(category.getId());
        insertVO.setCategoryName(category.getCategoryName());   //category_name 取类别表真实值
        insertVO.setBrands(toInsert);
        categoryMapper.associateBrand(insertVO);
    }

    /* 某类别已关联的品牌列表(编辑回显: 前端据此回打勾)
     * 联表查由 CategoryMapper.selectBrandsByCategoryId 提供, 只返回 id/brandName */
    @Override
    public List<Brand> listBrandsOfCategory(Integer categoryId) {
        if (categoryId == null) {
            return new ArrayList<>();
        }
        return categoryMapper.selectBrandsByCategoryId(categoryId);
    }

    private int levelOf(String pIds) {
        return pIds == null || pIds.trim().isEmpty() ? 1 : pIds.split(",").length;
    }

    private int subtreeHeight(GoodsCategory root) {
        String prefix = (root.getPIds() == null ? "0" : root.getPIds()) + "," + root.getId();
        QueryWrapper<GoodsCategory> qw = new QueryWrapper<>();
        qw.eq("p_ids", prefix).or(w -> w.likeRight("p_ids", prefix + ","));
        int rootLevel = levelOf(root.getPIds());
        int maxLevel = rootLevel;
        for (GoodsCategory sub : categoryMapper.selectList(qw)) {
            maxLevel = Math.max(maxLevel, levelOf(sub.getPIds()));
        }
        return maxLevel - rootLevel + 1;
    }

    private void requireName(GoodsCategory category) {
        if (category == null || category.getCategoryName() == null
                || category.getCategoryName().trim().isEmpty()) {
            throw new RuntimeException("类别名称不能为空");
        }
        category.setCategoryName(category.getCategoryName().trim());
    }
}

package com.gec.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gec.dao.DeptMapper;
import com.gec.domain.bo.DeptBO;
import com.gec.domain.entity.Dept;
import com.gec.domain.entity.Node;
import com.gec.service.IBaseService;
import com.gec.service.IDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DeptServiceImpl
    extends ServiceImpl<DeptMapper, Dept>
    implements IDeptService, IBaseService {
    @Autowired
    private DeptMapper deptMapper;

    /* ================================================================
     * 树形处理(即 IBaseService 注释中"把线性结构 ==> 层次结构"的实现,
     * 接口未给出方法签名, 故以私有方法的形式放在本实现类内)
     * ================================================================ */

    /*
     * 平铺部门列表 → 树:
     * 1.全部转成 DeptBO 放入 Map(id → 节点);
     * 2.遍历时把每个节点挂到父节点(parent_id)下;
     * 3.找不到父节点(parent_id=0/父部门不在数据里)的作为根(顶层部门);
     * 4.自顶向下填充 level(根=1 递增)与 hasSub(有子节点为 true)。
     */
    private List<Node> buildTree(List<Dept> deptList) {
        //1.按 id 升序转 BO 并放入 LinkedHashMap(保证后面的挂树顺序稳定)
        Map<Integer, DeptBO> nodeMap = new LinkedHashMap<>();
        for (Dept dept : deptList) {
            nodeMap.put(dept.getId(), new DeptBO(dept));
        }
        //2.挂树 + 收集根
        List<Node> roots = new ArrayList<>();
        for (DeptBO node : nodeMap.values()) {
            DeptBO parent = nodeMap.get(node.getParentId());
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
        List<Node> children = (node instanceof DeptBO)
                ? ((DeptBO) node).getChildren() : null;
        boolean hasSub = (children != null && !children.isEmpty());
        if (hasSub) {
            for (Node child : children) {
                fillLevelAndHasSub(child, level + 1);
            }
        }
        node.setHasSub(hasSub);
    }

    @Override
    public List<Node> listDept() {
        /*--请填入代码1--*/
        //1.查全部部门(id 升序, 树输出顺序稳定)
        List<Dept> list = deptMapper.selectList(
                new QueryWrapper<Dept>().orderByAsc("id"));
        //2.平铺 → 树
        return buildTree(list);
    }

    //在这里编写这个方法主要是为了适配不同类型。
    @Override
    public Node copyObj(Node N) {
        /*--请填入代码2--*/
        //1.只支持"部门实体 → 部门节点"的复制; 其他类型没有可用的拷贝目标
        if (!(N instanceof Dept)) {
            return null;
        }
        //2.利用 DeptBO 提供的 (Dept) 构造器完成字段复制
        return new DeptBO((Dept) N);
    }

    @Override
    public void deleteDept(Integer deptId) {
        /*--请填入代码3--*/
        //1.部门下还有用户 → 拒绝删除
        int userCount = deptMapper.getUserCount(deptId);
        if (userCount > 0) {
            throw new RuntimeException(
                "该部门下还有 " + userCount + " 个用户, 请先把用户调走再删除");
        }
        //2.部门下还有子部门 → 拒绝删除
        int subCount = deptMapper.getSubDeptCount(deptId);
        if (subCount > 0) {
            throw new RuntimeException(
                "该部门下还有 " + subCount + " 个子部门, 请先删除子部门");
        }
        //3.校验通过后物理删除
        deptMapper.deleteById(deptId);
    }

    /*
    *  ID           父ID   PIDS(上代所有 ID)
    *  21   人事部    2     0,1,2
    *  200  下级部门  21    0,1,2,21  [新的PIDS]
    * p_ids 生成规则与 mall_sys.sql 已有数据完全一致:
    *   顶层部门(父=0/null): p_ids = "0"
    *   子部门:              p_ids = 父部门.p_ids + "," + 父部门.id
    */
    private Dept setParentIds(Dept dept){
       /*--请填入代码4--*/
        Integer parentId = dept.getParentId();
        if (parentId == null || parentId <= 0) {
            //1.作为顶层部门: parent_id 归 0, p_ids 固定 "0"
            dept.setParentId(0);
            dept.setPIds("0");
            return dept;
        }
        //2.父部门必须真实存在
        Dept parent = deptMapper.selectById(parentId);
        if (parent == null) {
            throw new RuntimeException("父部门(id=" + parentId + ")不存在, 无法创建子部门");
        }
        if (levelOf(parent.getPIds()) >= 4) {
            throw new RuntimeException("部门最多只能创建 4 级");
        }
        //3.子部门 p_ids = 父部门 p_ids + "," + 父部门 id
        String parentPIds = parent.getPIds();
        if (parentPIds == null || parentPIds.trim().isEmpty()) {
            parentPIds = "0";   //父数据异常时兜底, 视为挂在根下
        }
        dept.setPIds(parentPIds + "," + parentId);
        return dept;
    }

    @Override
    public void addDept(Dept dept) {
        /*--请填入代码5--*/
        //1.校验父部门存在与否 + 生成 p_ids
        requireName(dept);
        setParentIds(dept);
        //2.入库(自增 id 由 MyBatis-Plus 回填到 dept.id)
        deptMapper.insert(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(Dept dept) {
        /*--请填入代码6--*/
        //1.被修改的部门必须存在
        Dept old = deptMapper.selectById(dept.getId());
        if (old == null) {
            throw new RuntimeException("要修改的部门不存在(id=" + dept.getId() + ")");
        }
        requireName(dept);
        boolean parentChanged = !Objects.equals(old.getParentId(), dept.getParentId());
        //旧路径前缀: 直属子部门的 p_ids 恰等于它, 更深子孙以它开头
        String oldPrefix = (old.getPIds() == null ? "0" : old.getPIds())
                + "," + old.getId();

        if (parentChanged) {
            //2.防环: 新父部门不能是它自己, 也不能是它自己的子孙
            Integer newParentId = dept.getParentId();
            if (newParentId != null && newParentId > 0) {
                Dept newParent = deptMapper.selectById(newParentId);
                if (newParent == null) {
                    throw new RuntimeException("父部门(id=" + newParentId + ")不存在");
                }
                String np = newParent.getPIds();
                if (np != null && (np.equals(oldPrefix) || np.startsWith(oldPrefix + ","))) {
                    throw new RuntimeException("不能把部门移动到它的下级部门下(会形成环)");
                }
            }
            int newLevel = newParentId == null || newParentId <= 0
                    ? 1 : levelOf(deptMapper.selectById(newParentId).getPIds()) + 1;
            if (newLevel + subtreeHeight(old) - 1 > 4) {
                throw new RuntimeException("移动后部门层级将超过 4 级");
            }
            //3.自身 p_ids 按新父部门重新生成(父部门为 null/0 时回到顶层)
            setParentIds(dept);
            //4.级联: 该分支所有子孙的 p_ids 前缀 旧→新(只更新受影响的分支)
            updateSubTreePIds(oldPrefix, dept.getPIds() + "," + dept.getId());
        } else {
            //5.父部门没变: p_ids 沿用库里的旧值(不信任前端提交的 p_ids)
            dept.setPIds(old.getPIds());
        }
        //6.更新部门本体
        deptMapper.updateById(dept);
    }

    /*
     * 级联更新子孙 p_ids 前缀:
     * 子孙 = p_ids 恰等于 oldPrefix 的(直属子部门) + 以 "oldPrefix," 开头的(更深层)。
     * 每个子孙去掉旧前缀, 换成 newPrefix, 后半段原样保留。
     */
    private void updateSubTreePIds(String oldPrefix, String newPrefix) {
        QueryWrapper<Dept> qw = new QueryWrapper<>();
        qw.eq("p_ids", oldPrefix).or(w -> w.likeRight("p_ids", oldPrefix + ","));
        List<Dept> subs = deptMapper.selectList(qw);
        for (Dept sub : subs) {
            String suffix = sub.getPIds().substring(oldPrefix.length());
            sub.setPIds(newPrefix + suffix);
            deptMapper.updateById(sub);
        }
    }

    @Override
    public Dept getDept(Integer id) {
        return deptMapper.selectById(id);
    }

    private int levelOf(String pIds) {
        return pIds == null || pIds.trim().isEmpty() ? 1 : pIds.split(",").length;
    }

    private int subtreeHeight(Dept root) {
        String prefix = (root.getPIds() == null ? "0" : root.getPIds()) + "," + root.getId();
        QueryWrapper<Dept> qw = new QueryWrapper<>();
        qw.eq("p_ids", prefix).or(w -> w.likeRight("p_ids", prefix + ","));
        int rootLevel = levelOf(root.getPIds());
        int maxLevel = rootLevel;
        for (Dept sub : deptMapper.selectList(qw)) {
            maxLevel = Math.max(maxLevel, levelOf(sub.getPIds()));
        }
        return maxLevel - rootLevel + 1;
    }

    private void requireName(Dept dept) {
        if (dept == null || dept.getDeptName() == null || dept.getDeptName().trim().isEmpty()) {
            throw new RuntimeException("部门名称不能为空");
        }
        dept.setDeptName(dept.getDeptName().trim());
    }
}

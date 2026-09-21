package com.gec.service;

/*
 * 主要用途: (通用节点)
 * 1. 处理树形节点的数据(解析, 封装)。
 * 2. 组织相关数据, 生成树形结构。
 * 3. 甚至可以实现查找功能。
 * 4. 对一个树形结构进行 增, 删, 改.
 *
 * [+]一级部门: 总经办
 *    [+]二级部门: 行政部
 *                人事部
 *    [+]三级部门
 *
 * [+]一级部门: 公司工会(委员会)
 *    [+]二级部门: 行政部
 * [+]一级部门: 董事会
 *
 */
import com.gec.domain.entity.Node;

/*
 * [说明] 资料里本接口注释声称有"方法1/方法2"但并未写出签名——
 * 接口保持原样不扩展(DeptServiceImpl implements 它并无编译障碍),
 * 树形"线性→层次"的组装、level/hasSub 填充逻辑直接实现在 DeptServiceImpl 内部私有方法中。
 */
public interface IBaseService {
    /*
    * 1.原本的 list 没有分层结构处理的。
    * 2.这里给它做一下分层处理, 转换为带分层结构的列表。
    * 一句话: 把线性结构 ==> 层次结构。
    */

    /* 1.请实现方法1. */
    /* (此方法在 DeptServiceImpl 内部以 buildTree(...) 私有方法实现) */

	/* 2.请实现方法2. */
    /* (level / hasSub 等节点附加属性的填充, 在 DeptServiceImpl 内部以 fillLevelAndHasSub(...) 实现) */

    //这个方法是在实现类中实施。
    Node copyObj(Node node);
}
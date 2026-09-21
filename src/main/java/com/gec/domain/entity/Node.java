package com.gec.domain.entity;

/*
 * 树形节点通用接口(资料原样):
 * 实体(Dept)与 BO(DeptBO)都可以实现它, 统一被树形逻辑(List<Node>)处理。
 * 全部方法是 default: 让"只当叶子/只当节点载体"的实现不必被迫写一堆空方法。
 */
public interface Node {
    default Integer getId(){ return null; }
    default Integer getParentId(){ return null; }
    default void addChildNode(Node node){}
    default Node copyNode(Node node){ return null; }
    default void clearEmptyChild(){ }

    //{2}新增两组方法
    default Integer getLevel(){ return null; }
    default void setLevel(Integer level){}

    default boolean getHasSub(){ return false; }
    default void setHasSub(boolean flag){}
}
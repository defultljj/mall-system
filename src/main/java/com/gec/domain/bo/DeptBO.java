package com.gec.domain.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gec.domain.entity.Dept;
import com.gec.domain.entity.Node;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/*
 * 部门 BO: 部门树的"节点载体"(树形展示用)。
 *  - children: 子部门列表, 为空时 JSON 不输出
 *  - level:    层级(根=1), 由 DeptServiceImpl 建树时填充
 *  - hasSub:   是否有下级部门, 由 DeptServiceImpl 建树时填充
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DeptBO implements Node {
    private Integer id;
    private String deptName;     //dept_name
    private String deptDesc;     //dept_desc
    private Integer parentId;    //parent_id
    private String pIds;         //p_ids

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Node> children = new ArrayList<>();

    //附加属性: 层级(根部门为 1)
    private Integer level;
    @Override
    public Integer getLevel() {
        return level;
    }
    @Override
    public void setLevel(Integer level) {
        this.level = level;
    }

    //附加属性: 是否有下级部门(叶子为 false, 前端用于展开/折叠)
    private Boolean hasSub;
    @Override
    public boolean getHasSub() {
        return Boolean.TRUE.equals(hasSub);
    }
    @Override
    public void setHasSub(boolean hasSub) {
        this.hasSub = hasSub;
    }

    public DeptBO() {}
    public DeptBO(Dept d) {
        this.id = d.getId();
        this.deptName = d.getDeptName();
        this.parentId = d.getParentId();
        this.pIds = d.getPIds();
        this.deptDesc = d.getDeptDesc();
    }
    public void addChildNode(Node node){
        children.add( node );
    }
}
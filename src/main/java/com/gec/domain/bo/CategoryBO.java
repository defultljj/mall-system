package com.gec.domain.bo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gec.domain.entity.GoodsCategory;
import com.gec.domain.entity.Node;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/*
 * 类别 BO: 类别树的"节点载体"(树形展示用), 与部门树节点 DeptBO 同款。
 *  - children: 子类别列表, 为空时 JSON 不输出
 *  - level:    层级(根=1), 由 CategoryServiceImpl 建树时填充
 *  - hasSub:   是否有下级类别, 由 CategoryServiceImpl 建树时填充
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CategoryBO implements Node {
    private Integer id;
    private String categoryName;    //category_name 类别名称
    private Integer parentId;       //parent_id 父类别 id(0=顶层)
    private String pIds;            //p_ids 上代 id 链(JSON 输出 "pids", 与部门树一致即可)

    private Integer showStatus;     //show_status 1=启用 0=停用
    private Integer sort;           //sort 排序号
    private String icons;           //icons 图标
    private String productUnit;     //product_unit 商品单位
    private Integer productCount;   //product_count 商品数量

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Node> children = new ArrayList<>();

    //附加属性: 层级(根类别为 1)
    private Integer level;
    @Override
    public Integer getLevel() {
        return level;
    }
    @Override
    public void setLevel(Integer level) {
        this.level = level;
    }

    //附加属性: 是否有下级类别(叶子为 false, 前端用于展开/折叠)
    private Boolean hasSub;
    @Override
    public boolean getHasSub() {
        return Boolean.TRUE.equals(hasSub);
    }
    @Override
    public void setHasSub(boolean hasSub) {
        this.hasSub = hasSub;
    }

    public CategoryBO() {}
    public CategoryBO(GoodsCategory c) {
        this.id = c.getId();
        this.categoryName = c.getCategoryName();
        this.parentId = c.getParentId();
        this.pIds = c.getPIds();
        this.showStatus = c.getShowStatus();
        this.sort = c.getSort();
        this.icons = c.getIcons();
        this.productUnit = c.getProductUnit();
        this.productCount = c.getProductCount();
    }
    public void addChildNode(Node node){
        children.add( node );
    }
}

package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/*
 * 商品类别实体 → 对应 tbl_goods_category 表(树形, 有自引用外键 parent_id → id)。
 * 结构与部门实体 Dept 同款(p_ids 存"上代所有类别 id 链", 如 "0,1,10")。
 * show_status 语义: 1=启用 0=停用(库中默认 1)。
 * 与部门表的最大差异: 本表有自引用外键, parent_id 不允许存 0,
 * 所以顶层类别入库必须为 NULL(存量数据里历史遗留的 0 是导入脚本关外键写入的,
 * 读取时 0/null 一律按顶层处理; 新增/移动代码见 CategoryServiceImpl.setParentIds)。
 */
@Data
@TableName(value="tbl_goods_category")
@EqualsAndHashCode(callSuper = false)
public class GoodsCategory extends BaseEntity implements Node {
    @TableId(type=IdType.AUTO)
    private Integer id;
    private String categoryName;    //category_name 类别名称

    /*
     * parent_id 父类别 id(入参语义: 缺省=不变, 0=顶层, >0=指定父)。
     * updateStrategy=IGNORED: 允许 updateById 时把本列显式更新为 NULL ——
     * 把类别移到顶层(0)时需要把 parent_id 从原值置回 NULL,
     * 而 MyBatis-Plus 默认策略会忽略 null 字段不更新, 故这里必须放开。
     */
    @TableField(value="parent_id", updateStrategy = FieldStrategy.IGNORED)
    private Integer parentId;       //parent_id 父类别 id(0=顶层)

    @TableField(value="p_ids")
    private String pIds;            //p_ids 上代所有类别 id 链

    private Integer showStatus;     //show_status 状态 1=启用 0=停用
    private Integer sort;           //sort 排序号(数值小在前)
    private String icons;           //icons 类别图标
    private String productUnit;     //product_unit 商品单位(如: 台/件)
    private Integer productCount;   //product_count 商品数量(冗余统计)
}

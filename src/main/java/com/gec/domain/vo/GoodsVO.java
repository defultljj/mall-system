package com.gec.domain.vo;

import com.gec.domain.entity.SpuDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/*
 * 商品管理列表/详情行 VO(9.12 新增, 对应前端"商品管理"视图的契约)。
 * 直接 extends SpuDetail: 字段与 tbl_spu_detail 列一一对应
 * (id/goodsName/goodsDetails/categoryId/brandId/weight/mainImg/allowDeduct/rewardPoint
 *  + 继承 BaseEntity 的 createDate/updateDate), 只额外补两个"联表才拿得到"的名称字段:
 *  - categoryName tbl_goods_category.category_name(列表"所属类别"列)
 *  - brandName    tbl_brand.brand_name(列表/详情"品牌"列)
 * 多继承两个字段的代价只是 Java 单继承限制, 换来的是与 SpuDetail 零字段漂移
 * (SpuDetail 加列时这里自动跟上), 与 BrandVO 的"实体 + 视图字段"做法等价。
 * 注意: 本类只作查询结果(resultType)使用, 不参与 insert/update ——
 *       继承来的 @TableName/@TableId 对纯 MyBatis 列映射没有影响。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GoodsVO extends SpuDetail {
    private String categoryName;  //联表: 所属类别名称
    private String brandName;     //联表: 所属品牌名称
    private BigDecimal minPrice;  //启用 SKU 的最低实际售价(优惠价优先)
    private BigDecimal maxPrice;  //启用 SKU 的最高实际售价(优惠价优先)
}

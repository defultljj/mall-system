package com.gec.domain.vo;

import com.gec.domain.entity.SpuDetail;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 商品管理“编辑商品”的一次性提交契约。 */
@Data
public class GoodsEditVO {
    private SpuDetail spu;
    private List<AttrValueItem> attrValues = new ArrayList<>();
    private List<SkuItem> skus = new ArrayList<>();

    @Data
    public static class AttrValueItem {
        private Integer attrId;
        private Integer valueType;
        private String attrValue;
    }

    @Data
    public static class SkuItem {
        private Integer id;
        private String skuName;
        private String skuCode;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private Integer stock;
        private String skuStatus;
        private String skuImg;
        private List<SkuAttrItem> attrs = new ArrayList<>();
    }

    @Data
    public static class SkuAttrItem {
        private Integer attrId;
        private String attrValue;
    }
}

package com.gec.domain.vo;

import lombok.Data;

/*
 * 商品图集 VO(9.11 资料原样): 发布商品第 1 步前端提交/回显用。
 * images 保持 String[](JSON 数组, 每个元素是 /PublishGoods/showImg/album/xxx 形式的地址)。
 */
@Data
public class SpuAlbumVO
    implements java.io.Serializable{
    private String goodsId;
    private String skuId;
    private String[] images;
}

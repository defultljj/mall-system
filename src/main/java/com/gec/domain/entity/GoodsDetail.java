package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/*
 * 商品详情实体(9.11 资料: 发布商品第 1 步"基本信息"的 Redis 缓存载体)。
 *
 * [缺陷修复]
 *  - mainImage 加 @TableField("main_img"): 表列名是 main_img, 而 "mainImage" 这种
 *    "单词+驼峰" 字段推不出下划线列名, 不显式标注的话 select 出来 mainImage 恒为 null。
 *    (注: 本工程里真正落库的实体是 SpuDetail, 它也有同样的列, 已在那边处理)
 *
 * [定位说明]
 *  - 本类本期只作为 Redis 缓存载体: 前端把基本信息提交到 /PublishGoods/saveGoodsBaseInfo,
 *    由 GoodsDetailServiceImpl 序列化成 JSON 存进 Redis, 暂不落库;
 *    落库统一走 SpuDetail(表 tbl_spu_detail)。
 *  - createDate/updateDate 保持 String(照抄资料): 仅用于缓存传输时的展示格式,
 *    不是 BaseEntity 的 LocalDateTime 自动填充字段; 落库时间由 SpuDetail 负责。
 */
@Data
@TableName(value="tbl_spu_detail")
public class GoodsDetail
    implements java.io.Serializable {
    @TableId(type= IdType.AUTO)
    private Integer id;
    private String goodsName;
    private String goodsDetails;
    private Integer categoryId;
    private Integer brandId;
    @TableField("main_img")   //修复: 显式绑定列名 main_img(否则含本字段的查询会映射不到)
    private String mainImage;
    private String weight;
    private String createDate;
    private String updateDate;

}

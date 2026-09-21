package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/*
 * 商品图集实体(9.11 资料: 发布商品第 1 步的 Redis 缓存载体之一)。
 * [缺陷修复] @TableName 由资料里的 "tbl_point_rule" 改为 "tbl_spu_album"(明显复制粘贴错误)。
 *
 * [本期定位] 仅作 Redis 缓存载体, 随发布流程暂存到 Redis, 暂不落库。
 * tbl_spu_album 建表等后续资料, 本模块零 DDL。
 * 注: 前端提交/回显走的是 SpuAlbumVO(images 为 String[]), 本实体只用于映射表结构。
 */
@Data
@TableName(value="tbl_spu_album")
public class SpuAlbum {
    @TableId(type= IdType.AUTO)
    private String id;
    private String goodsId;
    private String skuId;
    private String images;
    private String createDate;
    private String updateDate;

}

package com.gec.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/*
 * 积分规则实体(9.11 资料: 发布商品第 1 步的 Redis 缓存载体之一)。
 * 字段: goodsId 商品id / rewardPoint 返还积分 / allowDeduct 是否允许抵扣 /
 *       deductMaxRate 最大抵扣比例 / status 状态 / createDate+updateDate 展示用字符串。
 *
 * [本期定位] 仅作 Redis 缓存载体, 随即发布流程暂存到 Redis, 暂不落库。
 * tbl_point_rule 建表等后续资料, 本模块零 DDL。
 * (照抄资料: createDate/updateDate 保持 String, 仅用于缓存传输)
 */
@Data
@TableName(value="tbl_point_rule")
public class PointRule implements java.io.Serializable {
    @TableId(type= IdType.AUTO)
    private Integer id;
    private Integer goodsId;
    private Integer rewardPoint;
    private Integer allowDeduct;
    private Double deductMaxRate;
    private String status;
    private String createDate;
    private String updateDate;

}

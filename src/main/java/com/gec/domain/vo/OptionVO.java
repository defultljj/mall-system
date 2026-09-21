package com.gec.domain.vo;

import lombok.Data;

/*
 * 下拉选项 VO: 后端给前端下拉框用的通用结构 {value, label}。
 */
@Data
public class OptionVO {
    private Integer value;
    private String label;
}
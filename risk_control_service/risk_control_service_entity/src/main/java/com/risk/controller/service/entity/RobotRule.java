package com.risk.controller.service.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RobotRule {
    private Long id;
    private String name;//模型名称
    private String desc;//模型描述
    private String handler;//模型执行方法，格式:$package.class.method
    private BigDecimal percent;//百分比
    private Long enabled;//1启用，0停用
    private String setting;//辅助规则
}

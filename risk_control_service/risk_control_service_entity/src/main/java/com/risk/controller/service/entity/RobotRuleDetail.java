package com.risk.controller.service.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RobotRuleDetail {
    private Long id;//
    private Long ruleId;//模型id
    private BigDecimal minScope;//范围最小值>=
    private BigDecimal maxScope;//<范围最大值
    private BigDecimal goodPercent;//好户概率
    private BigDecimal overduePercent;//首逾概率
    private Long enabled;//1启用，0停用

    private Long totalCnt;//总个数
    private Long goodCnt;//好户个数
    private Long overdueCnt;//首逾个数
}

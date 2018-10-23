package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;


@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ModelUserScore extends BaseEntity {
    private Long userId;//
    private String nid;//订单号
    private Long ruleId;//规则id
    private BigDecimal subScore;//扣分值
    private BigDecimal score;//最终得分
}

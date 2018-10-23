package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ModelGroupRule extends BaseEntity {

    private static final Long RULE_1001 = 10001001L;
    private static final Long RULE_1002 = 10001002L;
    private static final Long RULE_1003 = 10001003L;

    private Long groupId; //模型规则id
    private Long ruleId; //模型对应的明细id
    private BigDecimal weight;//权重
    private Integer enabled;// 1启用，0停用
}

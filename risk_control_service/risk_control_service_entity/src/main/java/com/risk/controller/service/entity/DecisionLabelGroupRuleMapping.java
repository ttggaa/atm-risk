package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DecisionLabelGroupRuleMapping extends BaseEntity {
    private Long groupId;
    private Integer stage;
    private Long ruleId;
    private Integer priority; // 执行优先级，值大的优先执行
    private Integer enabled; // 是否在用，1-是
    private Integer robotAction;
}

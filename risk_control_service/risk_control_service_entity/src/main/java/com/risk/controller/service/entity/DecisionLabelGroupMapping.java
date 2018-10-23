package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DecisionLabelGroupMapping extends BaseEntity {
    private Long groupId;
    private Long labelId;
    private Integer enabled; // 是否在用，1-是
}

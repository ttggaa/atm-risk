package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DecisionLabel extends BaseEntity {
    private String name;
    private String shortName;
    private String code;
    private String remark;
    private Integer enabled; // 是否在用，1-是
    private Long merchantId;
}

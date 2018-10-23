package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ThirdServiceCfg extends BaseEntity {
    private String name;
    private String value;
    private String remark;
}

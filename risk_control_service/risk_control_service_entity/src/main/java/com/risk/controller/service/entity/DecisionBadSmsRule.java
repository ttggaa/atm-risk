package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Table;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Table(name = "risk_decision_bad_sms_rule")
public class DecisionBadSmsRule extends BaseEntity {
    public final static String DIRECTION_ALL = "all";
    public final static String DIRECTION_IN = "in";
    public final static String DIRECTION_SENT = "sent";

    private String name;
    private String direction;
    private String ruleRegexp;
    private Integer enabled;
    private String remark;
}

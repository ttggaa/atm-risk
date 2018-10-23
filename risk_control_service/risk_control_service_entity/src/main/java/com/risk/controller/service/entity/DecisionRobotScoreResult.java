package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Created by Administrator on 2016/11/15.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DecisionRobotScoreResult extends BaseEntity {
    private String nid;
    private Integer status;
    private BigDecimal probOf1;
    private BigDecimal probOf0;

    private String errorMsg;
    private Long startTime;
    private Long endTime;
}

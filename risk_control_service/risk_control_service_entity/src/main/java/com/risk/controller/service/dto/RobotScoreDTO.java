package com.risk.controller.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Created by root on 6/2/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)

public class RobotScoreDTO extends BaseDto {
    private String nid;
    private Integer status; // 1: successful 2: fail
    private BigDecimal probOf1; // 风险分, 取值：[0,1]，0-风险最小, 1-风险最大
    private BigDecimal probOf0;

    private Integer preliminaryResult; // 初审结果， 1-拒绝, 0-通过
    private Integer finalResult; // 终审结果， 1-拒绝, 0-通过

    private String errorMsg;
    private Long startTime;
    private Long endTime;
}

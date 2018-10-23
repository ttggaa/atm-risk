package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Created by Administrator on 2016/11/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DecisionRobotResult extends BaseEntity {
    private String nid;
    private Integer admissionResult; //结果状态，1-成功，2-失败
    private BigDecimal probOf1;//风险分范围最小值
    private Long ruleId;
    private Integer result;
}

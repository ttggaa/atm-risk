package com.risk.controller.service.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RobotResult {
    private Long id;
    private String nid;//订单号
    private Long ruleId;//规则id
    private Long detailId;//规则明细id
    private Object num;//规则对应的个数
    private Integer result;//是否满足，1满足，0不满足
    private Long addTime;
    private Long updateTime;

    public RobotResult() {
    }

    public RobotResult(String nid, Long ruleId, Long detailId, Object num, Integer result) {
        this.nid = nid;
        this.ruleId = ruleId;
        this.detailId = detailId;
        this.num = num;
        this.result = result;
    }
}

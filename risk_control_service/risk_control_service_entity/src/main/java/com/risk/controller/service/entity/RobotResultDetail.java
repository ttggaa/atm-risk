package com.risk.controller.service.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RobotResultDetail {
    private Long id;
    private Long resultId;//订单号
    private Long detailId;//规则明细id
    private Object num;//规则对应的个数
    private Integer result;//是否满足，1满足，0不满足
    private Long addTime;
    private Long updateTime;

    public RobotResultDetail() {
    }

    public RobotResultDetail( Long detailId, Object num, Integer result) {
        this.detailId = detailId;
        this.num = num;
        this.result = result;
    }
}

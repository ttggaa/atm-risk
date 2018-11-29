package com.risk.controller.service.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
public class DecisionWhiteList {
    private Long id;//
    private String merchant_code;//商户号
    private String phone; // '手机号码'
    private Integer enabled; // 1:启用，0停用
    private Date addTime;
}

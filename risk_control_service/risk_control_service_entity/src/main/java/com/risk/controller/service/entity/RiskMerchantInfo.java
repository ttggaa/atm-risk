package com.risk.controller.service.entity;

import lombok.Data;

@Data
public class RiskMerchantInfo {
    private Long id;
    private String code;//商户代码,
    private String name;//商户名称,
    private Integer enabled;//1启用，0停用
    private String remark;//备注
}

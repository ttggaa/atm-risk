package com.risk.controller.service.entity;

import lombok.Data;

@Data
public class RiskMerchantUrl {
    private Long id;
    private String merchantCode;//商户代码,
    private String key;//商户名称,
    private String url;//1启用，0停用
    private String remark;//备注
}

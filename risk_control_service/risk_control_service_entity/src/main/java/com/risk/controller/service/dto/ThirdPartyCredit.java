package com.risk.controller.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 第三方增信数据响应类
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThirdPartyCredit implements Serializable {
    private Integer credooScore;

    private Long gatherTime;

    private Map<String, Object> jxl_call_records;

    private List qianhaiOftenLoan;

    private List risk_items;

    private List qh_riskmark;
}

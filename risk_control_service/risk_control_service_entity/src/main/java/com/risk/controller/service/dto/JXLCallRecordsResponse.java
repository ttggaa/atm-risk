package com.risk.controller.service.dto;

import lombok.Data;

/**
 * 聚信立通话记录
 */
@Data
public class JXLCallRecordsResponse {
    private String updateTime;
    private String startTime;
    private String initType;
    private Long useTime;
    private String place;
    private String otherCellPhone;
    private String cellPhone;
    private Long subtotal;
    private String callType;

}
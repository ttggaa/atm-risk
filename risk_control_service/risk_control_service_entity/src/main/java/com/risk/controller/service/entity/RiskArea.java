package com.risk.controller.service.entity;

import java.util.List;

import lombok.Data;

@Data
public class RiskArea extends BaseEntity {
    private String name;
    private String nid;//简称
    private Integer pid;//级别（省市区县）
    private Integer code;//代码
    private Integer enabled;//是否启用
    private Integer level;//级别
    
    private List<Integer> codes;//代码
    
    private Integer provinceCode;//代码
    private Integer cityCode;//代码
    private Integer distinctCode;//代码


}

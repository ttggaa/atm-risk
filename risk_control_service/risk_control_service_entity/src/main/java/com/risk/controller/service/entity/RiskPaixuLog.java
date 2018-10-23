package com.risk.controller.service.entity;

import lombok.Data;

@Data
public class RiskPaixuLog extends BaseEntity {

    private String phone;//请求代码
    private String idNo;//身份证号码
    private String idName;//身份证号码
    private String reqParam;// 请求参数
    private Long reqTime;// 请求时间
    private Long reqCnt;//请求耗时
    private String repParam; //响应结果
    private Integer status;//0:初始状态，1：请求成功，2请求失败
    private String nid;//请求订单号唯一
    private String rspCode;//错误代码
    private String result;//模型结果
    private Double score;//模型分数
}

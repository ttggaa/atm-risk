package com.risk.controller.service.utils.paixu.entity;

import com.alibaba.fastjson.JSONObject;

import lombok.Data;


@Data
public class PreApplyWithCreditVO extends ControllerBaseVO {
    /**
     *  资产方订单编号
     */
    private String orderId;

    /**
     * 用户姓名
     */
    private String userName;


    /**
     * 身份证号
     */
    private String cardNum;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 进件数据
     */
    private JSONObject riskData;
}
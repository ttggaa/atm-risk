package com.risk.controller.service.dao;

public interface DecisionWhiteListDao {

    /**
     * 查询是否白名单
     *
     * @param merchantCode 商户号
     * @param phone        手机号码
     * @return
     */
    int getByPhone(String merchantCode, String phone);
}
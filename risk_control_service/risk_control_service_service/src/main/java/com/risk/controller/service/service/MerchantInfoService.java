package com.risk.controller.service.service;

/**
 * description
 *
 * @Author: Tonny
 * @CreateDate: 18/11/27 上午 11:38
 * @Version: 1.0
 */
public interface MerchantInfoService {


    /**
     * 通过商户号获取缓存中的商户信息
     *
     * @param merchantCode
     * @return
     */
    String getBymerchantCode(String merchantCode);

    /**
     * 通过商户号和url的key获取url
     * @param merchantCode
     * @param urlKey
     * @return
     */
    String getMerchantUrl(String merchantCode, String urlKey);
}

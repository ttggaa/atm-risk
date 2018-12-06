package com.risk.controller.service.service.impl;

import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.service.MerchantInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * description
 *
 * @Author: Tonny
 * @CreateDate: 18/11/27 上午 11:37
 * @Version: 1.0
 */
@Slf4j
@Service
public class MerchantInfoServiceImpl implements MerchantInfoService {
    @Autowired
    private LocalCache localCache;

    @Override
    public String getBymerchantCode(String merchantCode) {
        return localCache.getLocalCache(CacheCfgType.MERCHANT_INFO, merchantCode);
    }

    @Override
    public String getMerchantUrl(String merchantCode, String urlKey) {
        return localCache.getLocalCache(CacheCfgType.MERCHANT_INFO, merchantCode + urlKey);
    }
}

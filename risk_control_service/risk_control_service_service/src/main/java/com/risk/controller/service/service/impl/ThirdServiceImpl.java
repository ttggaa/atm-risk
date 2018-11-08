package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.service.ThirdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 第三方接口类
 */
@Service
@Slf4j
public class ThirdServiceImpl implements ThirdService {
    @Autowired
    private LocalCache localCache;

    /**
     * 查询用户的设备被多少个用户使用
     *
     * @param userId
     * @return
     */
    @Override
    public JSONObject getDeviceUsedCount(Long userId) {
        if (null != userId && 0L != userId) {
            String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.deviceUsage.url");
            Map<String, String> params = new HashMap<>();
            params.put("userId", String.valueOf(userId));
            try {
                String resultStr = HttpClientUtils.doPost(url, JSONObject.toJSONString(params), "application/json");
                JSONObject json = JSONObject.parseObject(resultStr);
                return json;
            } catch (Throwable e) {
                log.error("查询用户的设备被多少个用户使用异常，userId:{},e:{}", userId, e);
            }
        }
        return null;
    }

    @Override
    public JSONObject getRegisterCount(Set<String> set) {
        if (null == set || set.size() > 0) {
            String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.registerCount.url");
            try {
                String resultStr = HttpClientUtils.doPost(url, JSONObject.toJSONString(set), "application/json");
                JSONObject json = JSONObject.parseObject(resultStr);
                return json;
            } catch (Throwable e) {
                log.error("查询用户的设备被多少个用户使用异常，set:{},e:{}", set, e);
            }
        }
        return null;
    }

    /**
     * 查询用户设备个数
     *
     * @param userId
     * @return
     */
    @Override
    public JSONObject getDeviceCount(Long userId) {
        if (null != userId && 0L != userId) {
            String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.userDeviceUsage.url");
            Map<String, String> params = new HashMap<>();
            params.put("userId", String.valueOf(userId));
            try {
                String resultStr = HttpClientUtils.doPost(url, JSONObject.toJSONString(params), "application/json");
                JSONObject json = JSONObject.parseObject(resultStr);
                return json;
            } catch (Throwable e) {
                log.error("查询设备个数异常，userId:{},e:{}", userId, e);
            }
        }
        return null;
    }
}

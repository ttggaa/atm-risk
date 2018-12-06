package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.service.MerchantInfoService;
import com.risk.controller.service.service.ThirdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
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
    @Autowired
    private MerchantInfoService merchantInfoService;

    /**
     * 查询用户的设备被多少个用户使用
     *
     * @param userId
     * @return
     */
    @Override
    public JSONObject getDeviceUsedCount(String merchantCode,Long userId) {
        if (null != userId && 0L != userId) {
            String url = merchantInfoService.getMerchantUrl(merchantCode, "device_used_num_url");
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
    public JSONObject getRegisterCount(String merchantCode, Set<String> set) {
        if (null == set || set.size() > 0) {
            String url = merchantInfoService.getMerchantUrl(merchantCode, "register_num_url");
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
    public JSONObject getDeviceCount(String merchantCode,Long userId) {
        if (null != userId && 0L != userId) {
            String url = merchantInfoService.getMerchantUrl(merchantCode, "user_device_num_url");
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

    @Override
    public JSONObject getUserInfo(String merchantCode, Long userId) {
        if (null != userId && 0L != userId) {
            String url = merchantInfoService.getMerchantUrl(merchantCode, "user_info_url");
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

    /**
     * 查询用户的通讯录、最近90天通话记录的手机号码，逾期个数
     * @param phones
     * @param overdueDay
     * @return
     */
    @Override
    public JSONObject queryCntOptPhoneOverdueNum(String merchantCode,Set<String> phones, Integer overdueDay) {
        if (null == phones || phones.size() > 0) {
            String url = merchantInfoService.getMerchantUrl(merchantCode, "overdue_num_url");

            Map<String,Object> param = new HashMap<>();
            param.put("phones",phones);
            param.put("overdueDay",overdueDay);
            try {
                String resultStr = HttpClientUtils.doPost(url, JSONObject.toJSONString(param), "application/json");
                JSONObject json = JSONObject.parseObject(resultStr);
                return json;
            } catch (Throwable e) {
                log.error("查询用户的通讯录、最近90天通话记录的手机号码，逾期个数异常，phones:{},e:{}", param, e);
            }
        }
        return null;
    }

    @Override
    public void repeatAddOperator(String merchantCode, String nid) {
        if (StringUtils.isNotBlank(nid)) {
            String url = merchantInfoService.getMerchantUrl(merchantCode, "repeat_operator_url");

            Map<String, String> param = new HashMap<>();
            param.put("nids", nid);
            try {
                String resultStr = HttpClientUtils.doPost(url, param);
                if (StringUtils.isBlank(resultStr)) {
                    log.error("调用重新拉取运营商数据失败");
                }
            } catch (Throwable e) {
                log.error("调用重新拉取运营商数据异常，phones:{},e:{}", param, e);
            }
        }
    }
}

package com.risk.controller.service.service;

import com.alibaba.fastjson.JSONObject;

import java.util.Set;

/**
 * 第三方接口
 */
public interface ThirdService {

    /**
     * 查询用户的设备被多少个用户使用
     * @param userId
     * @return
     */
    JSONObject getDeviceUsedCount(Long userId);

    /**
     * 查询注册个数
     * @param set
     * @return
     */
    JSONObject getRegisterCount(Set<String> set);

    /**
     * 查询用户设备个数
     * @param userId
     * @return
     */
    JSONObject getDeviceCount(Long userId);
}

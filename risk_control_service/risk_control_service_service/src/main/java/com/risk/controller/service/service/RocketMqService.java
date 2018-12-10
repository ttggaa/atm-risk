package com.risk.controller.service.service;

import org.springframework.stereotype.Component;


@Component
public interface RocketMqService {


    /**
     * 模型计费MQ生产者
     *
     * @param appId
     * @param phone
     */
    public void zxmodel(String appId, String phone);

    /**
     * 决策计费MQ生产者
     *
     * @param appId
     * @param phone
     */
    public void decisionEngine(String appId, String phone);
}

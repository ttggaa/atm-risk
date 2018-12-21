package com.risk.controller.service.service.impl;

import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.service.RocketMqService;
import com.zxsh.merchant.message.CallEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Slf4j
@Component
public class RocketMqServiceImpl implements RocketMqService{

    @Resource
    private CallEventPublisher callEventPublisher;
    @Autowired
    private LocalCache localCache;

    /**
     * 模型计费MQ生产者
     *
     * @param appId
     * @param phone
     */
    @Override
    public void zxmodel(String appId, String phone) {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(phone)) {
            return;
        }
        String switchOn = localCache.getLocalCache(CacheCfgType.SYSTEMCFG, "risk.model.bill.mq.switch");
        if ("1".equals(switchOn)) {
            log.info("发送模型计费MQ:appid:{},phone:{}", appId, phone);
            callEventPublisher.publishRiskEvent(appId, "ZXMODEL", "针信模型", phone);
        } else {
            log.warn("模型计费MQ开关关闭，值：" + switchOn);
        }
    }

    /**
     * 决策计费MQ生产者
     *
     * @param appId
     * @param phone
     */
    @Override
    public void decisionEngine(String appId, String phone) {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(phone)) {
            return;
        }
        String switchOn = localCache.getLocalCache(CacheCfgType.SYSTEMCFG, "risk.decision.bill.mq.switch");
        if ("1".equals(switchOn)) {
            log.info("发送决策计费MQ:appid:{},phone:{}", appId, phone);
            callEventPublisher.publishRiskEvent(appId, "DECISIONENGINE", "决策引擎", phone);
        } else {
            log.warn("决策计费MQ关闭，值：" + switchOn);
        }
    }

}

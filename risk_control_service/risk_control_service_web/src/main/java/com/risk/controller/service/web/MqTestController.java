package com.risk.controller.service.web;

import com.risk.controller.service.service.RocketMqService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * description
 *
 * @Author: Tonny
 * @CreateDate: 18/12/18 下午 02:15
 * @Version: 1.0
 */
@RequestMapping("mq")
@Controller
public class MqTestController {
    @Resource
    private RocketMqService rocketMqService;

    @RequestMapping(value = "send", method = RequestMethod.GET)
    @ResponseBody
    public String event(String type, String appId, String phone) {
        type = StringUtils.isBlank(type) ? "1" : type;
        appId = StringUtils.isBlank(appId) ? "app181203141643105230" : appId;
        phone = StringUtils.isBlank(phone) ? "18888888888" : phone;
        if ("1".equals(type)) {
            rocketMqService.zxmodel(appId, phone);
        } else {
            rocketMqService.decisionEngine(appId, phone);
        }
        return "OK";
    }
}

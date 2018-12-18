package com.risk.service.verify;

import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.service.RocketMqService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * Created by zhangtong on 2018/4/11 0011.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBootStart.class)
public class MqTest {
    @Resource
    private RocketMqService rocketMqService;

    @Test
    public void event() {
        rocketMqService.zxmodel("app181203141643105230", "18888888888");
    }

    @Test
    public void event2() {
        rocketMqService.decisionEngine("app181203141643105230", "18888888888");
    }

}

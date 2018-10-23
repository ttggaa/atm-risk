package com.risk.service.verify;

import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.RiskControlServiceService;
import com.risk.controller.service.service.XinyanService;
import com.risk.controller.service.utils.xinyan.dto.XinyanRadarParamDTO;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

/**
 * @author SHUQWEI
 * @Pagke Name   com.risk.service.verify
 * @Description
 * @create 2018-08-27
 **/
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBootStart.class)
public class DecisionRuleServcieTest {

    @Resource
    private RiskControlServiceService riskControlServiceService;

    @Resource
    private XinyanService xinyanService;

    @Test
    public void testDecisionNew() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setLabelGroupId(1000L);
        request.setNid("118090310174944751");
        request.setCardId("500240199110320159");
        request.setUserName("18883315432");
        request.setUserId(44L);
        request.setIsRobot(1);
        request.setFailFast(1);
        request.setDevicePlatform("ios");
        request.setName("张三");
        riskControlServiceService.decisionHandle(request);
    }

    @Test
    public void getRadarApply() {
        XinyanRadarParamDTO param = new XinyanRadarParamDTO();
        param.setIdName("张三");
        param.setIdNo("142702180001062719");
        xinyanService.getRadarApply(param, false);
        xinyanService.getRadarBehavior(param, false);
    }
    @Test
    public void reRunDecision() {
        for(int i=0;i<5;i++){
            riskControlServiceService.reRunDecision();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

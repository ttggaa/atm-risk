package com.risk.service.verify;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.handler.VerifyHandler;
import com.risk.controller.service.handler.XinyanHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhangtong on 2018/4/11 0011.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBootStart.class)
public class XinyanTest {
    @Resource
    private XinyanHandler xinyanHandler;
    @Test
    public void verifyXinyanData() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setCardId("411322198911080346");
        request.setName("赵明会");
        request.setUserName("18211808095");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String,String> set = new HashMap<>();
        set.put("minCount","5");
        rule.setSetting(set);

        AdmissionResultDTO record = xinyanHandler.verifyXinyanData(request, rule);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("===========================");
    }

    @Test
    public void verifyXinyanApplyData() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setCardId("522627199205170415");
        request.setName("陆曙");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String,String> set = new HashMap<>();
        set.put("minScore","5");
        rule.setSetting(set);

        AdmissionResultDTO record = xinyanHandler.verifyXinyanApplyData(request, rule);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("===========================");
    }

    @Test
    public void verifyXinyanBehaviorData() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setCardId("522627199205170415");
        request.setName("陆曙");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String,String> set = new HashMap<>();
        set.put("minScore","700");
        rule.setSetting(set);

        AdmissionResultDTO record = xinyanHandler.verifyXinyanBehaviorData(request, rule);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("===========================");
    }

}

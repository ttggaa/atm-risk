package com.risk.service.verify;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.handler.ShuMeiHandler;
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
public class ShumeiNewTest {
    @Resource
    private ShuMeiHandler shuMeiHandler;

    @Test
    public void verifyShumeiData() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("118090421162493996");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String,String> set = new HashMap<>();
        set.put("minCount","3");
        rule.setSetting(set);

        AdmissionResultDTO record2 = shuMeiHandler.verifyShumeiData(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verifyShumeiReject() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218090716122631329");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String,String> set = new HashMap<>();
        set.put("minCount","2");
        rule.setSetting(set);

        AdmissionResultDTO record2 = shuMeiHandler.verifyShumeiReject(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verifyShumeApply() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218090716122631329");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String,String> set = new HashMap<>();
        set.put("minCount","2");
        rule.setSetting(set);

        AdmissionResultDTO record2 = shuMeiHandler.verifyShumeApply(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

}

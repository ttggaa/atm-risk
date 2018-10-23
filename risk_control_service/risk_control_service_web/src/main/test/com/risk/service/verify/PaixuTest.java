package com.risk.service.verify;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.handler.PaixuHandler;
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
public class PaixuTest {
    @Resource
    private PaixuHandler paixuHandler;
    @Test
    public void verifyPaixuDecision() {
        DecisionHandleRequest request = new DecisionHandleRequest();

        String str = "{\"applyTime\":1538122930142,\"cardId\":\"340321199210061846\",\"devicePlatform\":\"ios\",\"failFast\":1,\"isRobot\":1,\"labelGroupId\":1001,\"name\":\"年竹青\",\"nid\":\"218092816221049359\",\"userId\":20,\"userName\":\"15000297289\",\"userNation\":\"汉\"}";
        request = JSONObject.parseObject(str,DecisionHandleRequest.class);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String,String> set = new HashMap<>();
        set.put("checkScore","460");
        set.put("passScore","530");
        set.put("excludeScore","-777");
        rule.setSetting(set);

        AdmissionResultDTO record = paixuHandler.verifyPaixuDecision(request, rule);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("===========================");
    }

}

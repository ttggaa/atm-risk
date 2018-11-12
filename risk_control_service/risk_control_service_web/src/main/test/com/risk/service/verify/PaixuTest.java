package com.risk.service.verify;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dao.AdmissionResultDao;
import com.risk.controller.service.dao.AdmissionRuleDao;
import com.risk.controller.service.dao.DecisionReqLogDao;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.entity.AdmissionResult;
import com.risk.controller.service.entity.AdmissionRule;
import com.risk.controller.service.entity.DecisionReqLog;
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
    @Resource
    private DecisionReqLogDao decisionReqLogDao;
    @Test
    public void verifyPaixuDecision() {
        DecisionHandleRequest request = new DecisionHandleRequest();

        DecisionReqLog result = new DecisionReqLog();
        request.setUserName("18883315455");
        request.setCardId("500200198910100111");
        request.setNid("2180928162210493591");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String,String> set = new HashMap<>();
        set.put("checkScore","460");
        set.put("passScore","530");
        set.put("excludeScore","-777");
        set.put("randomNum","100");
        set.put("passPercent","0.31");
        set.put("passCount","14");
        rule.setSetting(set);

        AdmissionResultDTO record = paixuHandler.verifyPaixuDecision(request, rule);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("===========================");
    }

}

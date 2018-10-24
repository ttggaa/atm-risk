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
public class CallEachTest {
    @Resource
    private VerifyHandler verifyHandler;

    @Test
    public void verifyXinyanData2() {
        //{"amount":720,"applyTime":1540203002864,"cardId":"350781199007094415","devicePlatform":"android","failFast":1,"isRobot":1,"labelGroupId":1009,"name":"黄陆平","nid":"218102218100294838","productId":1,"robotRequestDTO":{},"userId":168,"userName":"13850900870","userNation":"汉"}
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218102218100294838");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("repeatPerson", "3");
        set.put("repeatNum", "3");
        rule.setSetting(set);

        AdmissionResultDTO record = verifyHandler.verifyCallsEach(request, rule);

        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("===========================");
    }

    @Test
    public void verifyXinyanData() {
        //{"amount":720,"applyTime":1540203002864,"cardId":"350781199007094415","devicePlatform":"android","failFast":1,"isRobot":1,"labelGroupId":1009,"name":"黄陆平","nid":"218102218100294838","productId":1,"robotRequestDTO":{},"userId":168,"userName":"13850900870","userNation":"汉"}
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218102218100294838");
        request.setApplyTime(1540203002864L);
        // {"callNum":"1","callDay":"7"}
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("callNum", "1");
        set.put("callDay", "7");
        rule.setSetting(set);

        AdmissionResultDTO record = verifyHandler.verifyCallsEach(request, rule);

        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("===========================");
    }
}

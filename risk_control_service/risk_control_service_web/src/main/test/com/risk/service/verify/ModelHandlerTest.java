package com.risk.service.verify;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.handler.ModelHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.ModelService;
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
public class ModelHandlerTest {
    @Resource
    private ModelHandler modelHandler;
    @Resource
    private ModelService modelService;

    @Test
    public void verifyUserDeviceCallDetail() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218092115564171117");
        request.setUserId(4L);
        request.setLabelGroupId(1000L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
//        set.put("score","20");
//        set.put("initScore","1000");
//        rule.setSetting(set);

        AdmissionResultDTO record2 = modelHandler.verifyUserOperator(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }


    @Test
    public void verifyUserOperator() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218091810415578876");
        request.setUserId(3303L);
        request.setLabelGroupId(1000L);
        request.setApplyTime(1537200000000L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("minPercent", "1.15");
        set.put("maxPercent", "1.6");

        set.put("minTimePercent", "0.5");
        set.put("maxTimePercent", "0.65");
        set.put("allCallNum30", "100");
        set.put("cntCallNum30", "10");
        set.put("num30Compare180", "0.1");
        set.put("time30Compare180", "0.1");
        rule.setSetting(set);

        AdmissionResultDTO record2 = modelHandler.verifyUserOperator(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

}

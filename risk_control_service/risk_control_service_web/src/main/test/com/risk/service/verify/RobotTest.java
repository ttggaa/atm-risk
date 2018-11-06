package com.risk.service.verify;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.handler.RobotHandler;
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
public class RobotTest {
    @Resource
    private RobotHandler robotHandler;
    @Resource
    private ModelService modelService;
    @Test
    public void robotHandler() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserName("18883315455");
        request.setCardId("500200198910100111");
        request.setNid("218091810415578876");
        request.setUserId(3303L);
        request.setLabelGroupId(1000L);
        request.setApplyTime(1537200000000L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String,String> set = new HashMap<>();
        set.put("passPercent","0.31");
        set.put("passCount","1");
        set.put("callNum","0");
        set.put("callDay","7");
        set.put("randomNum","100");
        rule.setSetting(set);

        AdmissionResultDTO record = robotHandler.verifyRobot(request, rule);

        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("===========================");
    }

    @Test
    public void modelLearn() {
//        modelService.modelLearn(23L);
        modelService.modelLearn(null);
    }
    @Test
    public void runModelBySql() {
        String sql = "select nid from risk_decision_req_log ";
        robotHandler.runModelBySql(sql);
    }

}

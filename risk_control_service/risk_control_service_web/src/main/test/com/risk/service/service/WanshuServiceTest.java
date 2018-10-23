package com.risk.service.service;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.entity.WanshuReqLog;
import com.risk.controller.service.handler.PaixuHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.WanshuService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by zhangtong on 2018/4/11 0011.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBootStart.class)
public class WanshuServiceTest {
    @Resource
    private WanshuService wanshuService;
    @Test
    public void verifyPaixuDecision() {
        WanshuReqLog str = wanshuService.yangmaodang("218091720110173292","18883315431");
        System.out.println(JSONObject.toJSONString(str));
    }

    @Test
    public void queryKonghao() {
        String ser = "18711499578,17373476680,13564563820,18817776572,18365942157,18263584408,18048969957,18664034601,17765135391,13601888235,15583671119,18188422333,13644062845,18624048358,15842282212,13591219138,13967078759,18611167825,15641279156,13007429742,13656356783,18606778343,18368316760,18634001110,13061759718,18680071056,15135688887,15201769942,15583676688,13840545883";
        String str[] = ser.split(",");
        Set<String> set = new HashSet<>(Arrays.asList(str));
        for (String s : set) {
//            WanshuReqLog wanshuReqLog = wanshuService.yangmaodang("",s);
//
//           WanshuReqLog wanshuReqLog2 = wanshuService.queryKonghao("218091720110173292", s);
        }
        System.out.println(JSONObject.toJSONString(str));
    }


}

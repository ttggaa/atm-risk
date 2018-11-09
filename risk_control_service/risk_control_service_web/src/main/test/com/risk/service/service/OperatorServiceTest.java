package com.risk.service.service;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.entity.WanshuReqLog;
import com.risk.controller.service.service.OperatorService;
import com.risk.controller.service.service.WanshuService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by zhangtong on 2018/4/11 0011.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBootStart.class)
public class OperatorServiceTest {
    @Resource
    private OperatorService operatorService;
    @Test
    public void verifyPaixuDecision() throws Exception {
        operatorService.saveAllOperator("31806021379960164");
    }


}

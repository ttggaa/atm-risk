package com.risk.service.dao;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dao.DataOrderMappingDao;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.entity.DataOrderMapping;
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
    private DataOrderMappingDao dao;
    @Test
    public void verifyPaixuDecision() {
        DataOrderMapping mapping = new DataOrderMapping();
        mapping.setUserId(1L);
        mapping.setNid("1");
        mapping = dao.queryLastOne(mapping);
        System.out.println(JSONObject.toJSONString(mapping));
    }

}

package com.risk.service.black;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.RejectReasonDao;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.dto.BlackListDTO;
import com.risk.controller.service.handler.VerifyHandler;
import com.risk.controller.service.mongo.dao.MongoDao;
import com.risk.controller.service.mongo.dao.MongoQuery;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.BlackListService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by zhangtong on 2018/4/11 0011.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBootStart.class)
public class BlackListTest {
    @Resource
    private BlackListService blackListService;
    @Resource
    private RejectReasonDao rejectReasonDao;

    @Test
    public void queryBlackList(){
        BlackListDTO dto = new BlackListDTO();
        dto.setPhones("18616252882,15821225045");
        dto.setIdCards("18880000023,15821225045");
        dto.setMacs("A4:50:46:FD:AD:69,15821225045");
        dto.setIdfas("858B34AB-D6BB-4643-AF76-8EBFFC193A0A,12345");
        dto.setImeis("123123123");
        ResponseEntity rs = blackListService.queryBlacklist(dto);
        System.out.println(JSONObject.toJSONString(rs));
    }

    @Test
    public void getMaxCloseDays() {
        Set<String> set = new HashSet<>();
        set.add("R401");
        Integer count = rejectReasonDao.getMaxCloseDays(set);
        System.out.println(String.valueOf(count));
    }
    @Test
    public void timerToNow(){
        System.out.println("now time:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }
}

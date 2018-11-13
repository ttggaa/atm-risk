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
        set.put("passScore","0.31");
        set.put("randomNum","100");
        rule.setSetting(set);

        AdmissionResultDTO record = robotHandler.verifyRobot(request, rule);

        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("===========================");
    }

    @Test
    public void robotAge() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotAge(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }

    @Test
    public void robotDeviceUsed() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotDeviceUsed(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }

    @Test
    public void robotDeviceCount() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotDeviceCount(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }

    @Test
    public void robotCntRegisterCount() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotCntRegisterCount(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }
    @Test
    public void robotCntCount() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotCntCount(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }

    @Test
    public void robotOptShortNum() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotOptShortNum(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }
    @Test
    public void robotOptPhoneUsedTime() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotOptPhoneUsedTime(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }
    @Test
    public void robotOptAvgFee() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotOptAvgFee(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }

    @Test
    public void robotOptRegisterNum() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotOptRegisterNum(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }

    @Test
    public void robotShumeiMultiNum7() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotShumeiMultiNum7(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotShumeiMultiNum30(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotShumeiMultiNum60(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotShumeiMultiNum(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }

    @Test
    public void robotCallManNum10() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");


    }

    @Test
    public void robotCntEachOtherNum10() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");

        Object count = robotHandler.robotCntCallManNum10(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntCallTime10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntCallNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntCalledNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");


        count = robotHandler.robotCntCalledTime10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");


        count = robotHandler.robotCntCalledManNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntEachOtherNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntEachOtherTime10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntEachOtherManNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");


        count = robotHandler.robotCntEachOtherPercent10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }


    @Test
    public void robotCntEachOtherNum30() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");

        Object count = robotHandler.robotCntCallManNum30(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntCallTime30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntCallNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntCalledNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");


        count = robotHandler.robotCntCalledTime30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");


        count = robotHandler.robotCntCalledManNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntEachOtherNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntEachOtherTime30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntEachOtherManNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");


        count = robotHandler.robotCntEachOtherPercent30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }


    @Test
    public void robotCntEachOtherNum60() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");

        Object count = robotHandler.robotCntCallManNum60(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntCallTime60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntCallNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntCalledNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");


        count = robotHandler.robotCntCalledTime60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");


        count = robotHandler.robotCntCalledManNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntEachOtherNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntEachOtherTime60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntEachOtherManNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");


        count = robotHandler.robotCntEachOtherPercent60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }



    @Test
    public void robotOptCallNum10() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");

        Object count = robotHandler.robotOptCallNum10(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCallTime10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCallManNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCalledNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCalledTime10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCalledManNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptTime10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptManNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptSmsSendNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptSmsReceiveNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptSmsManNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidTime10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidManNum10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidPercent10(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }


    @Test
    public void robotOptCallNum30() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");

        Object count = robotHandler.robotOptCallNum30(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCallTime30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCallManNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCalledNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCalledTime30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCalledManNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptTime30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptManNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptSmsSendNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptSmsReceiveNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptSmsManNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidTime30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidManNum30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidPercent30(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");
    }


    @Test
    public void robotOptCallNum60() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101913572114374");
        Object count = robotHandler.robotOptCallNum60(request);
        System.out.println("===========================");
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCallTime60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCallManNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCalledNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCalledTime60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptCalledManNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptTime60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptManNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptSmsSendNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptSmsReceiveNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotOptSmsManNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidTime60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidManNum60(request);
        System.out.println(JSONObject.toJSONString(count));
        System.out.println("===========================");

        count = robotHandler.robotCntValidPercent60(request);
        System.out.println(JSONObject.toJSONString(count));
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

    @Test
    public void saveDate () {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserName("17317600807");
        request.setNid("218110715285014286");
//        modelService.saveData(request);
    }

    @Test
    public void verifyRobot () {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserName("17317600807");
        request.setNid("218110715285014286");
//        robotHandler.robotCallRiskAnalysisCollection(request);
//        robotHandler.robotCallRiskAnalysisCreditCard(request);
//        robotHandler.robotCallRiskAnalysisLoan(request);
//        robotHandler.robotCallRiskAnalysisGov(request);
//        robotHandler.robotCallCheckBlackInfoScore(request);
//        robotHandler.robotCallCheckBlackInfoRouter(request);
//        robotHandler.robotCallCheckBlackInfoClass2Cnt(request);
//        robotHandler.robotCallSearchedOrgCnt(request);
//        robotHandler.robotCallIdcardWithOtherNames(request);
//        robotHandler.robotCallIdcardWithOtherPhones(request);
//        robotHandler.robotCallPhoneWithOtherNames(request);
//        robotHandler.robotCallPhoneWithOtherIdcards(request);
        robotHandler.robotCallContactRegion(request);
//        robotHandler.robotCallTripInfo(request);
//        robotHandler.robotCallDurationDetail(request);
//        robotHandler.robotCallMidnightTotalCnt(request);
//        robotHandler.robotCallMidnightUniqNumCnt(request);
//        robotHandler.robotCallMidnightDialCnt(request);
//        robotHandler.robotCallMidnightDialedCnt(request);
//        robotHandler.robotCallPhoneSilent(request);
    }

}

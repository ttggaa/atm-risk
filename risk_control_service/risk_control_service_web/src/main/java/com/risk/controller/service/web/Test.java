package com.risk.controller.service.web;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.handler.RobotHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.ModelDataService;
import com.risk.controller.service.service.ModelService;
import com.risk.controller.service.utils.xinyan.dto.XinyanRadarParamDTO;
import com.risk.controller.service.service.XinyanService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class Test {

    @Autowired
    private ModelDataService modelDataService;
    @Autowired
    private RobotHandler robotHandler;

    @RequestMapping(value = "/test")
    @ResponseBody
    public ResponseEntity getRadarApply(XinyanRadarParamDTO param, String expire) throws Exception {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserName("17317600807");
        request.setNid("218110715285014286");
//        System.out.println(robotHandler.robotCallRiskAnalysisCollection(request));
//        System.out.println(robotHandler.robotCallRiskAnalysisCreditCard(request));
//        System.out.println(robotHandler.robotCallRiskAnalysisLoan(request));
//        System.out.println(robotHandler.robotCallRiskAnalysisGov(request));
//        System.out.println(robotHandler.robotCallCheckBlackInfoScore(request));
//        System.out.println(robotHandler.robotCallCheckBlackInfoRouter(request));
//        System.out.println(robotHandler.robotCallCheckBlackInfoClass2Cnt(request));
//        System.out.println(robotHandler.robotCallSearchedOrgCnt(request));
//        System.out.println(robotHandler.robotCallIdcardWithOtherNames(request));
//        System.out.println(robotHandler.robotCallIdcardWithOtherPhones(request));
//        System.out.println(robotHandler.robotCallPhoneWithOtherNames(request));
//        System.out.println(robotHandler.robotCallPhoneWithOtherIdcards(request));
        System.out.println(robotHandler.robotCallContactRegion(request));
//        System.out.println(robotHandler.robotCallTripInfo(request));
//        System.out.println(robotHandler.robotCallDurationDetail(request));
//        System.out.println(robotHandler.robotCallMidnightTotalCnt(request));
//        System.out.println(robotHandler.robotCallMidnightUniqNumCnt(request));
//        System.out.println(robotHandler.robotCallMidnightDialCnt(request));
//        System.out.println(robotHandler.robotCallMidnightDialedCnt(request));
//        System.out.println(robotHandler.robotCallPhoneSilent(request));
        return null;
    }

}

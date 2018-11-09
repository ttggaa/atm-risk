package com.risk.controller.service.dao;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;


public interface RiskModelOperatorReportDao {

    int genRwTime(Map<?, ?> param);

    int genSmBorrow(Map<?, ?> param);

    int genCallRiskAnalysis(Map<?, ?> param);

    int genBasicCheckItem(Map<?, ?> param);

    int genCheckBlackInfo (JSONObject params);

    int genCallFamilyDetail (JSONObject params);

    int genCallMidnight (JSONObject params);

    int genCallSilentAreasBill (JSONObject params);

    List<String> getAllReqNid();

    List<String> getSmBorrow();

    List<String> getRiskAnaLysisNid();

    List<String> getBasicCheckItem();

    List<String> getCheckBlackInfo();

    List<String> getCallFamilyDetail();

}
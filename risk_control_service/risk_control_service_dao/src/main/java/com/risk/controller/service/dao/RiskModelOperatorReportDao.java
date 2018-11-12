package com.risk.controller.service.dao;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;


public interface RiskModelOperatorReportDao {

    int saveRwTime(Map<?, ?> param);

    int saveSmBorrow(Map<?, ?> param);

    int saveCallRiskAnalysis(List<?> param);

    int saveBasicCheckItem(Map<?, ?> param);

    int saveCheckBlackInfo (JSONObject params);

    int saveCallFamilyDetail (JSONObject params);

    int saveCallFamilyDetail(List<?> param);

    int saveCallMidnight (JSONObject params);

    int saveCallSilentAreas (JSONObject params);

    List<String> getAllReqNid();

    List<String> getSmBorrow();

    List<String> getRiskAnaLysisNid();

    List<String> getBasicCheckItem();

    List<String> getCheckBlackInfo();

    List<String> getCallFamilyDetail();

}
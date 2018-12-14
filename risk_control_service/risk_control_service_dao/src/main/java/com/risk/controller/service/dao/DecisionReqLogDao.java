package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionReqLog;

import java.util.List;

public interface DecisionReqLogDao {
    void saveOrUpdate(DecisionReqLog log);

    DecisionReqLog getbyNid(String nid);

    DecisionReqLog getByNidAndMerchantCode(String nid, String merchantCode);

    /**
     * 查询1个小时之前挂起的数据查询30条
     * @return
     */
    List<DecisionReqLog> queryNeedReRun();
}
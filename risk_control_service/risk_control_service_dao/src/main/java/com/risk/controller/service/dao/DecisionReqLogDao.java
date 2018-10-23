package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionReqLog;

public interface DecisionReqLogDao {
    void saveOrUpdate(DecisionReqLog log);

    DecisionReqLog getbyNid(String nid);
}
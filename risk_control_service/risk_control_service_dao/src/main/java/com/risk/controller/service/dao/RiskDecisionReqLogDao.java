package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RiskDecisionReqLog;

public interface RiskDecisionReqLogDao {

	RiskDecisionReqLog getLastBynid(String nid);
    
}
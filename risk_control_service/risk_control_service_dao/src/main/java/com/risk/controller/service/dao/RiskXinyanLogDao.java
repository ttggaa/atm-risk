package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RiskXinyanLog;

public interface RiskXinyanLogDao {

    RiskXinyanLog getLastOne(String idNo, Integer reqTypeApply);

    void insert(RiskXinyanLog reqLog);
}
package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RiskPaixuLog;

import java.util.Map;

public interface RiskPaixuLogDao {

    void insert(RiskPaixuLog reqLog);

    Map<String, Object> getPaixu (String nid);
}
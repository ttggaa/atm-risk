package com.risk.controller.service.dao;

import java.util.List;

import com.risk.controller.service.entity.RiskArea;

public interface RiskAreaDao {

    List<RiskArea> getByCodes(RiskArea area);
    
}
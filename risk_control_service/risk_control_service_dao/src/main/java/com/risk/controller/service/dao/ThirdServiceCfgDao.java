package com.risk.controller.service.dao;

import com.risk.controller.service.entity.ThirdServiceCfg;

import java.util.List;

public interface ThirdServiceCfgDao {

    List<ThirdServiceCfg> getAll();

    ThirdServiceCfg getOne(ThirdServiceCfg thirdServiceCfgFind);
}
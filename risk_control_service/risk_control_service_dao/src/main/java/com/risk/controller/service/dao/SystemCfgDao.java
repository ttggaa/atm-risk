package com.risk.controller.service.dao;


import com.risk.controller.service.entity.SystemCfg;

import java.util.List;

public interface SystemCfgDao {
    void setSysteCfg(SystemCfg systeCfg);

    List<SystemCfg> getAll();

    SystemCfg getOne(SystemCfg systemCfgFind);
}
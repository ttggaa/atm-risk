package com.risk.controller.service.dao;

import com.risk.controller.service.entity.WanshuReqLog;

public interface WanshuReqLogDao {
    WanshuReqLog getLogByPhone(WanshuReqLog log);

    int insert(WanshuReqLog log);
}
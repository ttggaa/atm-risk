package com.risk.controller.service.dao;

import com.risk.controller.service.entity.StaOperatorCalls;

import java.util.List;

public interface StaOperatorCallsDao {
    void saveOrUpdateBatch(List<StaOperatorCalls> operatorCalls);
}
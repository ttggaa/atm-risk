package com.risk.controller.service.dao;

import com.risk.controller.service.entity.StaOperatorCalls;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StaOperatorCallsDao {
    void saveOrUpdateBatch(List<StaOperatorCalls> operatorCalls);

    List<StaOperatorCalls> getByNid(@Param("nid") String nid);
}
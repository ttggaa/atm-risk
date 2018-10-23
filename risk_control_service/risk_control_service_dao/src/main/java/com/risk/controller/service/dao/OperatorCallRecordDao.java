package com.risk.controller.service.dao;

import com.risk.controller.service.entity.OperatorCallRecord;

import java.util.List;

public interface OperatorCallRecordDao {
    void saveBatch(List<OperatorCallRecord> list);
}
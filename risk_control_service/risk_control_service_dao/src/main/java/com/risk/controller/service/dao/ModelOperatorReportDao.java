package com.risk.controller.service.dao;

import com.risk.controller.service.entity.ModelOperatorReport;

import java.util.List;

public interface ModelOperatorReportDao {
    void saveBatch(List<ModelOperatorReport> listResult);
}
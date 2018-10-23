package com.risk.controller.service.dao;

import com.risk.controller.service.entity.ModelRuleDetail;

import java.util.List;

public interface ModelRuleDetailDao {
    List<ModelRuleDetail> getAll(Long ruleId);
}
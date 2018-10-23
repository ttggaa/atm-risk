package com.risk.controller.service.dao;

import com.risk.controller.service.entity.ModelRuleDetailScope;

import java.util.List;

public interface ModelRuleDetailScopeDao {
    List<ModelRuleDetailScope> getScopeByRuleId(Long ruleId);
}
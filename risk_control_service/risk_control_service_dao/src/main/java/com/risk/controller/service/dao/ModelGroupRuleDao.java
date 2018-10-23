package com.risk.controller.service.dao;

import com.risk.controller.service.entity.ModelGroupRule;

import java.util.List;

public interface ModelGroupRuleDao {
    List<ModelGroupRule> getByGroupId(Long labelGroupId);
}
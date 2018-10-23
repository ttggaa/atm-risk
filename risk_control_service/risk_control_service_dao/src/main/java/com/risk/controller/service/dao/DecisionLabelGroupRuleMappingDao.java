package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionLabelGroupRuleMapping;

public interface DecisionLabelGroupRuleMappingDao {
    int deleteByPrimaryKey(Long id);

    int insert(DecisionLabelGroupRuleMapping record);

    int insertSelective(DecisionLabelGroupRuleMapping record);

    DecisionLabelGroupRuleMapping selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DecisionLabelGroupRuleMapping record);

    int updateByPrimaryKey(DecisionLabelGroupRuleMapping record);
}
package com.risk.controller.service.dao;

import com.risk.controller.service.entity.AdmissionRule;
import com.risk.controller.service.entity.DecisionLabelGroupRuleMapping;

import java.util.List;

public interface AdmissionRuleDao {
    int deleteByPrimaryKey(Long id);

    int insert(AdmissionRule record);

    int insertSelective(AdmissionRule record);

    AdmissionRule selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(AdmissionRule record);

    int updateByPrimaryKey(AdmissionRule record);

    List<AdmissionRule> getEnabledRuleByGroup(DecisionLabelGroupRuleMapping cond);

    /**
     * 通过ruleId查询规则
     * @param ruleId
     * @return
     */
    AdmissionRule getByRuleId(long ruleId);
}
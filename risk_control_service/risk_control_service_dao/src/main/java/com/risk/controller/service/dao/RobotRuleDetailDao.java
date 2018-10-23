package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RobotRuleDetail;

import java.util.List;

public interface RobotRuleDetailDao {
    RobotRuleDetail getDetailByCondition(Long robotId, Object condition);

    List<RobotRuleDetail> getAllEnabled();

    void updateAllSetZero();

    void updateBatchById(List<RobotRuleDetail> ruleDetailsList);
}
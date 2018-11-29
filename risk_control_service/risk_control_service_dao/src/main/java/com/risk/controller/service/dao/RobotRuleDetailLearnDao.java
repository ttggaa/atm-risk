package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RobotRuleDetail;
import com.risk.controller.service.entity.RobotRuleDetailLearn;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RobotRuleDetailLearnDao {
    RobotRuleDetailLearn getDetailByCondition(Long robotId, Object condition);

    List<RobotRuleDetailLearn> getAllEnabled();

    void updateAllSetZero(@Param(value = "ruleId") Long ruleId);

    void updateBatchById(List<RobotRuleDetailLearn> ruleDetailsList);
}
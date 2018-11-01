package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RobotRuleDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RobotRuleDetailDao {
    RobotRuleDetail getDetailByCondition(Long robotId, Object condition);

    List<RobotRuleDetail> getAllEnabled();

    void updateAllSetZero(@Param(value="ruleId")Long ruleId);

    void updateBatchById(List<RobotRuleDetail> ruleDetailsList);
}
package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RobotRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RobotRuleDao {
    List<RobotRule> getAllrobotRule(@Param(value="ruleId") Long ruleId);
}
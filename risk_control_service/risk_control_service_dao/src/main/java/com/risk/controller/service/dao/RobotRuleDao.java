package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RobotRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RobotRuleDao {
    /**
     * 查询模型维度
     *
     * @param ruleId  维度ID，为空查询所有维度
     * @param enabled 1：启用，0停用，为空查询所有
     * @return
     */
    List<RobotRule> getAllrobotRule(@Param(value = "ruleId") Long ruleId, @Param(value = "enabled") Integer enabled);
}
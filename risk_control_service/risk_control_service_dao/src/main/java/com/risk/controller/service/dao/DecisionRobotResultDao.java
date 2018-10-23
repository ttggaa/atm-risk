package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionRobotResult;

public interface DecisionRobotResultDao {
    int deleteByPrimaryKey(Long id);

    int insert(DecisionRobotResult record);

    int insertSelective(DecisionRobotResult record);

    DecisionRobotResult selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DecisionRobotResult record);

    int updateByPrimaryKey(DecisionRobotResult record);
}
package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionRobotScoreResult;

public interface DecisionRobotScoreResultDao {
    int deleteByPrimaryKey(Long id);

    int insert(DecisionRobotScoreResult record);

    int insertSelective(DecisionRobotScoreResult record);

    DecisionRobotScoreResult selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DecisionRobotScoreResult record);

    int updateByPrimaryKey(DecisionRobotScoreResult record);
}
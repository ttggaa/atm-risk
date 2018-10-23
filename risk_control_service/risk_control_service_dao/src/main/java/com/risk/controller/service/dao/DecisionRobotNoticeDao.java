package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionRobotNotice;

public interface DecisionRobotNoticeDao {
    int deleteByPrimaryKey(Long id);

    int insert(DecisionRobotNotice record);

    int insertSelective(DecisionRobotNotice record);

    DecisionRobotNotice selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DecisionRobotNotice record);

    int updateByPrimaryKey(DecisionRobotNotice record);

    DecisionRobotNotice getByNid(DecisionRobotNotice notice);
}
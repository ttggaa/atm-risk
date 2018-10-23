package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionLabelGroup;

public interface DecisionLabelGroupDao {
    int deleteByPrimaryKey(Long id);

    int insert(DecisionLabelGroup record);

    int insertSelective(DecisionLabelGroup record);

    DecisionLabelGroup selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DecisionLabelGroup record);

    int updateByPrimaryKey(DecisionLabelGroup record);
}
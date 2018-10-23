package com.risk.controller.service.dao;

import com.risk.controller.service.dto.DecisionGroupLabelDTO;
import com.risk.controller.service.entity.DecisionLabelGroupMapping;

import java.util.List;

public interface DecisionLabelGroupMappingDao {
    int deleteByPrimaryKey(Long id);

    int insert(DecisionLabelGroupMapping record);

    int insertSelective(DecisionLabelGroupMapping record);

    DecisionLabelGroupMapping selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DecisionLabelGroupMapping record);

    int updateByPrimaryKey(DecisionLabelGroupMapping record);

    List<DecisionGroupLabelDTO> getGroupLabelCsv(DecisionLabelGroupMapping cond);
}
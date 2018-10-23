package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionLabel;

public interface DecisionLabelDao {
    int deleteByPrimaryKey(Long id);

    int insert(DecisionLabel record);

    int insertSelective(DecisionLabel record);

    DecisionLabel selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DecisionLabel record);

    int updateByPrimaryKey(DecisionLabel record);

    DecisionLabel getByMerchantIdAndCode(DecisionLabel record);
}
package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionResultNotice;
import com.risk.controller.service.entity.DecisionRobotNotice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DecisionResultNoticeDao {
    int deleteByPrimaryKey(Long id);

    int insert(DecisionResultNotice record);

    DecisionResultNotice selectByPrimaryKey(Long id);

    int updateByPrimaryKey(DecisionRobotNotice record);

    int updateByPrimaryKeySelective(DecisionResultNotice record);

    DecisionResultNotice getByNid(@Param("nid") String nid);

    List<DecisionResultNotice> pushRiskResult(@Param("nid")String nid,@Param("noticeNum") Integer noticeNum);
}
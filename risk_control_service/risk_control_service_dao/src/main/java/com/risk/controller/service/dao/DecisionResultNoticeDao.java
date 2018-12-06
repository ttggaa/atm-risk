package com.risk.controller.service.dao;

import com.risk.controller.service.entity.DecisionResultNotice;
import com.risk.controller.service.entity.DecisionRobotNotice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DecisionResultNoticeDao {

    int insert(DecisionResultNotice record);

    int updateByPrimaryKeySelective(DecisionResultNotice record);

    DecisionResultNotice selectByCondition(DecisionResultNotice notice);

    List<DecisionResultNotice> pushRiskResult(@Param("nid")String nid,@Param("noticeNum") Integer noticeNum);
}
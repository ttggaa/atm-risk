package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RobotResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface RobotResultDao {
    void saveBatch(List<RobotResult> listRobot);

    /**
     * 查询所有好户坏户的订单
     *
     * @return
     */
    List<Map<String, Object>> queryRepayOrder();

    List<Map<String, Object>> runModelBySql(@Param("sql") String sql);
}
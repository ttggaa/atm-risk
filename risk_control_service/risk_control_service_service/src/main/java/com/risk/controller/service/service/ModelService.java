package com.risk.controller.service.service;

import com.risk.controller.service.entity.StaUserBaseinfo;
import com.risk.controller.service.request.DecisionHandleRequest;

import java.util.List;
import java.util.Map;

public interface ModelService {

    /**
     * 通过nid，或者类型，计算用户运营商数据
     *
     * @param nid
     * @param type
     */
    void verifyUserOperator(String nid, Integer type);

    /**
     * 训练模型
     * @param ruleId
     */
    void modelLearn(Long ruleId);

    /**
     * 查询最近10天主叫通话次数
     *
     * @param nid       订单号
     * @param applyTime 订单时间
     * @param day       最近天数
     * @param type      DIAL:主叫，DIALED被叫
     * @return
     */
    Map<String, Object> getCallNumByDay(String nid, Long applyTime, Integer day, String type);

    /**
     * 查询最近天数内互相通话次数和时长
     *
     * @param nid       订单号
     * @param applyTime 订单时间
     * @param day       最近天数
     * @return
     */
    Map<String, Object> getCallAndCalledByDay(String nid, Long applyTime, Integer day);

    /**
     * 通过sql注入动态查询nid列表
     *
     * @param sql
     * @return
     */
    List<Map<String, Object>> runModelBySql(String sql);

}

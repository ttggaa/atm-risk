package com.risk.controller.service.service;

import com.risk.controller.service.entity.StaOperatorCalls;
import com.risk.controller.service.entity.StaSmBorrows;
import com.risk.controller.service.entity.StaUserBaseinfo;
import com.risk.controller.service.request.DecisionHandleRequest;

import java.util.List;

public interface ModelDataService {

    void saveData(DecisionHandleRequest request) throws Exception;

    /**
     * 通过订单号查询 StaUserBaseinfo
     * @param request
     * @return
     */
    StaUserBaseinfo getUserBaseInfo(DecisionHandleRequest request);

    /**
     * 通过订单号查询StaOperatorCalls
     * @param request
     * @return
     */
    List<StaOperatorCalls> getOperatorCalls(DecisionHandleRequest request);

    /**
     * 通过订单号查询树美多头信息
     * @param request
     * @return
     */
    StaSmBorrows getStaSmBorrows(DecisionHandleRequest request);
}

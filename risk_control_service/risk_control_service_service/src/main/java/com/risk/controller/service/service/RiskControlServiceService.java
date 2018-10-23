package com.risk.controller.service.service;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.request.DecisionHandleRequest; /**
 * 风控决策服务的业务层接口
 *
 * @Author ZT
 * @create 2018-08-27
 */
public interface RiskControlServiceService {

    /**
     * 风控决策服务处理入口
     * @param request
     * @return
     */
    ResponseEntity decisionHandle(DecisionHandleRequest request);

    void reRunDecision();

    /**
     * 查询决策通过或者拒绝具体原因
     * @param nid
     * @return
     */
    ResponseEntity getDecisionDetail(String nid);
}

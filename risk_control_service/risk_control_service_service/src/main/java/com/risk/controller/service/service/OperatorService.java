package com.risk.controller.service.service;

import com.risk.controller.service.request.DecisionHandleRequest;

/**
 * 提供运营商数据操作
 */
public interface OperatorService {

	public Integer robotCallAndCalledNum7(String nid, Long applyTime, Integer days);

	void saveAllOperator(String nid) throws Exception;
}

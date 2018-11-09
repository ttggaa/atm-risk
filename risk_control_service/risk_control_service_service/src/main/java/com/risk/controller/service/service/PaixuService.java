package com.risk.controller.service.service;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.request.DecisionHandleRequest;

import java.util.List;
import java.util.Map;

public interface PaixuService {
	
	ResponseEntity requestPaixu(DecisionHandleRequest request);

	List<JSONObject> getEqMongoData(Map<String, Object> queryMap, String collection);

}

package com.risk.controller.service.service;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.request.DecisionHandleRequest;

public interface PaixuService {
	
	ResponseEntity requestPaixu(DecisionHandleRequest request);

}

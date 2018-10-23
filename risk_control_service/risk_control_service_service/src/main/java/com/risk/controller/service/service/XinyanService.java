package com.risk.controller.service.service;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.utils.xinyan.dto.XinyanRadarParamDTO;

public interface XinyanService {
	
	ResponseEntity getRadarApply(XinyanRadarParamDTO param, boolean expire);
	
	ResponseEntity getRadarBehavior(XinyanRadarParamDTO param, boolean expire);

}

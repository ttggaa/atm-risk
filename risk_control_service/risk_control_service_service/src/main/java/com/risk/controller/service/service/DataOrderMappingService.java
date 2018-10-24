package com.risk.controller.service.service;

import com.risk.controller.service.entity.DataOrderMapping;

public interface DataOrderMappingService {

	DataOrderMapping getLastOneByUserIdAndNid(Long userId,String nid);

	DataOrderMapping getLastOneByNid(String nid);
}

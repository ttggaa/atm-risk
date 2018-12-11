package com.risk.controller.service.service;

import com.risk.controller.service.entity.DataOrderMapping;

public interface DataOrderMappingService {

	DataOrderMapping getLastOneByNid(String nid);
}

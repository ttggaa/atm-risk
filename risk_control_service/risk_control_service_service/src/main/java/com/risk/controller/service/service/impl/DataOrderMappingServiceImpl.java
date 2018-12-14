package com.risk.controller.service.service.impl;

import com.risk.controller.service.dao.DataOrderMappingDao;
import com.risk.controller.service.entity.DataOrderMapping;
import com.risk.controller.service.service.DataOrderMappingService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataOrderMappingServiceImpl implements DataOrderMappingService {

    @Autowired
    private DataOrderMappingDao dataOrderMappingDao;

    @Override
    public DataOrderMapping getLastOneByNid(String nid) {
        return dataOrderMappingDao.queryLastOneByNid(nid);
    }

    @Override
    public DataOrderMapping getByMerchantCodeAndNid(String nid, String merchantCode) {
        return dataOrderMappingDao.getByMerchantCodeAndNid(nid, merchantCode);
    }
}

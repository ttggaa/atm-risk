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
    public DataOrderMapping getLastOneByUserIdAndNid(Long userId, String nid) {
        if (null == userId || 0L == userId || StringUtils.isBlank(nid)) {
            return null;
        }
        DataOrderMapping mapping = new DataOrderMapping();
        mapping.setUserId(userId);
        mapping.setNid(nid);
        return dataOrderMappingDao.queryLastOne(mapping);
    }

    @Override
    public DataOrderMapping getLastOneByNid(String nid) {
        return dataOrderMappingDao.queryLastOneByNid(nid);
    }
}

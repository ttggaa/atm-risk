package com.risk.controller.service.service.impl;

import com.risk.controller.service.handler.MongoHandler;
import com.risk.controller.service.service.ModelDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 保存模型数据的service
 */
@Slf4j
@Service
public class ModelDataServiceImpl implements ModelDataService {

    @Autowired
    private MongoHandler mongoHandler;

    /**
     * 入口方法
     * @param nid
     */
    @Override
    public void saveData(String nid) {

    }
}

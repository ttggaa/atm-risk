package com.risk.controller.service.service.impl;

import com.risk.controller.service.dao.StaOperatorCallsDao;
import com.risk.controller.service.dao.StaUserBaseinfoDao;
import com.risk.controller.service.handler.MongoHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
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
    @Autowired
    private StaOperatorCallsDao staOperatorCallsDao;
    @Autowired
    private StaUserBaseinfoDao staUserBaseinfoDao;

    /**
     * 入口方法
     *
     * @param request
     */
    @Override
    public void saveData(DecisionHandleRequest request) throws Exception {
        this.saveOperatorCalls(request);
    }

    /**
     * 保存运营商通话记录
     *
     * @param request
     * @throws Exception
     */
    private void saveOperatorCalls(DecisionHandleRequest request) throws Exception {

    }
}

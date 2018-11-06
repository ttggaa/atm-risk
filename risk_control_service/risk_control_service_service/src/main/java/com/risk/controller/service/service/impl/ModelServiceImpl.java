package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.handler.ModelHandler;
import com.risk.controller.service.handler.MongoHandler;
import com.risk.controller.service.handler.RobotLearnHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ModelServiceImpl implements ModelService {

    @Autowired
    private AdmissionResultDao admissionResultDao;
    @Autowired
    private ModelHandler modelHandler;
    @Autowired
    private DecisionReqLogDao decisionReqLogDao;
    @Autowired
    private RobotResultDetailDao robotResultDetailDao;
    @Autowired
    private RobotLearnHandler robotLearnHandler;
    @Autowired
    private DataOrderMappingDao dataOrderMappingDao;
    @Autowired
    private MongoHandler mongoHandler;
    @Autowired
    private OperatorCallRecordDao operatorCallRecordDao;
    @Autowired
    private ClientContactDao clientContactDao;

    @Override
    @Async
    public void verifyUserOperator(String nid, Integer result) {
        // 1、查询数据
        AdmissionResult query = new AdmissionResult();
        query.setNid(nid);
        query.setResult(result);
        List<String> list = admissionResultDao.getAllByCondition(query);
        if (null != list && list.size() > 0) {
            for (String resultNid : list) {
                DecisionHandleRequest request = new DecisionHandleRequest();
                request.setNid(resultNid);
                DecisionReqLog decisionReqLog = decisionReqLogDao.getbyNid(resultNid);
                if (null != decisionReqLog) {
                    JSONObject jsonObject = JSONObject.parseObject(decisionReqLog.getReqData());
                    request.setUserId(jsonObject.getLong("userId"));
                    request.setLabelGroupId(jsonObject.getLong("labelGroupId"));
                    request.setApplyTime(jsonObject.getLong("applyTime"));
                }
                this.verifyUserOperatorAsync(request, null);
            }
        }
    }

    @Async
    private void verifyUserOperatorAsync(DecisionHandleRequest request, Object o) {
        modelHandler.verifyUserOperator(request, null);
    }

    @Override
    @Async
    public void modelLearn(Long ruleId) {
        // 查询订单
        List<Map<String, Object>> list = robotResultDetailDao.queryRepayOrder();
        if (null != list && list.size() > 0) {
            this.robotLearnHandler.robotLearnDetail(list, ruleId, false);
        }
    }
    @Override
    public Map<String, Object> getCallNumByDay(String nid, Long applyTime, Integer day, String dial) {
        if (StringUtils.isBlank(nid)
                || null == applyTime || 0 >= applyTime
                || null == day || 0 >= day) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("nid", nid);
        map.put("applyTime", applyTime);
        map.put("day", day);
        map.put("type", dial);
        return this.clientContactDao.getCallNumByDay(map);
    }

    @Override
    public Map<String, Object> getCallAndCalledByDay(String nid, Long applyTime, Integer day) {
        if (StringUtils.isBlank(nid)
                || null == applyTime || 0 >= applyTime
                || null == day || 0 >= day) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("nid", nid);
        map.put("applyTime", applyTime);
        map.put("day", day);
        return this.clientContactDao.getCallAndCalledByDay(map);
    }

    @Override
    public List<Map<String, Object>> runModelBySql(String sql) {
        try {
            return robotResultDetailDao.runModelBySql(sql);
        } catch (Exception e) {
            log.error("执行sql异常：sql:{}", sql, e);
        }
        return null;
    }

}

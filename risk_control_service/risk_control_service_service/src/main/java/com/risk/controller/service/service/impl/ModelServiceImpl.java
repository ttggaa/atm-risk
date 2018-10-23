package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.PhoneUtils;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.handler.ModelHandler;
import com.risk.controller.service.handler.MongoHandler;
import com.risk.controller.service.handler.RobotHandler;
import com.risk.controller.service.handler.RobotLearnHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private RobotResultDao robotResultDao;
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
    public void modelLearn() {
        // 查询订单
        List<Map<String, Object>> list = robotResultDao.queryRepayOrder();
        if (null != list && list.size() > 0) {

            for (Map<String, Object> map : list) {
                String nid = (String) map.get("orderNo");// 订单号
                try {
                    this.saveAllOperator(nid);
                } catch (Exception e) {
                    log.error("保存数据失败,订单号：nid:{},error:{}", nid, e);
                }
            }

            this.robotLearnHandler.robotLearnDetail(list, false);
        }
    }

    @Override
    public void saveAllOperator(String nid) {
        List<Map<String, String>> result = dataOrderMappingDao.getAllByNid(nid);
        if (null != result && result.size() > 0) {
            for (Map<String, String> map : result) {
                try {
                    String mapNid = map.get("nid");
                    String operatorNum = map.get("operatorNum");
                    String clientNum = map.get("clientNum");
                    if (StringUtils.isBlank(mapNid) || StringUtils.isBlank(operatorNum) || StringUtils.isBlank(clientNum)) {
                        continue;
                    }
                    this.saveOperatorCallDetailByNumber(operatorNum, mapNid);
                    this.saveUserContact(clientNum, mapNid);
                } catch (Exception e) {
                    log.error("保存数据异常，nid:{},error:{}", nid, e);
                }
            }
        }
    }

    @Override
    public void saveAllOperator() {
        log.warn("定时拉取运营商模型数据");
        this.saveAllOperator(null);
    }

    private void saveUserContact(String clientNum, String nid) {
        List<JSONObject> client = mongoHandler.getUserContact(clientNum);
        if (null != client && client.size() > 0) {
            List<ClientContact> list = new ArrayList<>();
            for (JSONObject json : client) {
                ClientContact contact = new ClientContact();
                contact.setNid(nid);
                // 替换emoji表情
                String name = json.getString("contacts");
                if (StringUtils.isNotBlank(name)) {
                    name = name.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "");
                    name = name.length() > 10 ? name.substring(0, 10) : name;
                }
                contact.setName(name);
                contact.setUserId(json.getLong("clientId"));
                contact.setPhone(json.getString("phone"));
                String contactPhone = PhoneUtils.cleanTel(json.getString("contactsPhone"));
                contactPhone = contactPhone.length() > 32 ? contactPhone.substring(0, 30) : contactPhone;
                contact.setContactsPhone(contactPhone);
                list.add(contact);
            }

            if (list.size() > 0) {
                clientContactDao.saveBatch(list);
            }
        }
    }

    private void saveOperatorCallDetailByNumber(String operatorNum, String nid) {
        List<JSONObject> operator = mongoHandler.getOperatorCallDetailByNumber(operatorNum);
        if (null != operator && operator.size() > 0) {
            List<OperatorCallRecord> list = new ArrayList<>();
            for (JSONObject json : operator) {
                OperatorCallRecord record = new OperatorCallRecord();
                record.setUserId(json.getLong("clientId"));
                record.setNid(nid);
                record.setPhone(json.getString("phone"));
                String peer_number = json.getString("peer_number");
                peer_number = peer_number.length() > 32 ? peer_number.substring(0, 30) : peer_number;
                record.setPeerNumber(peer_number);
                record.setLocationType(json.getString("dial_type"));
                record.setDuration(json.getLong("duration"));
                record.setTime(json.getString("time"));
                list.add(record);
            }

            if (list.size() > 0) {
                operatorCallRecordDao.saveBatch(list);
            }
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
    @Async
    public List<Map<String, Object>> runModelBySql(String sql) {
        try {
            return robotResultDao.runModelBySql(sql);
        } catch (Exception e) {
            log.error("执行sql异常：sql:{}", sql, e);
        }
        return null;
    }

}

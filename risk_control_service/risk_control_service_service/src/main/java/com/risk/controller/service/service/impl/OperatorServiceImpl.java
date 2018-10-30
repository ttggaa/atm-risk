package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.PhoneUtils;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.ClientContactDao;
import com.risk.controller.service.dao.DataOrderMappingDao;
import com.risk.controller.service.dao.OperatorCallRecordDao;
import com.risk.controller.service.dao.RiskXinyanLogDao;
import com.risk.controller.service.entity.ClientContact;
import com.risk.controller.service.entity.OperatorCallRecord;
import com.risk.controller.service.entity.RiskXinyanLog;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.handler.MongoHandler;
import com.risk.controller.service.mongo.dao.MongoCollections;
import com.risk.controller.service.mongo.dao.MongoDao;
import com.risk.controller.service.mongo.dao.MongoQuery;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.OperatorService;
import com.risk.controller.service.utils.Base64;
import com.risk.controller.service.utils.xinyan.common.XinyanConstant;
import com.risk.controller.service.utils.xinyan.dto.XinyanRadarParamDTO;
import com.risk.controller.service.utils.xinyan.util.RsaCodingUtil;
import com.risk.controller.service.utils.xinyan.util.XinyanUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class OperatorServiceImpl implements OperatorService {

    @Autowired
    private LocalCache localCache;
    @Autowired
    private DataOrderMappingDao dataOrderMappingDao;
    @Autowired
    private ClientContactDao clientContactDao;
    @Autowired
    private MongoDao mongoDao;
    @Autowired
    private MongoHandler mongoHandler;
    @Autowired
    private OperatorCallRecordDao operatorCallRecordDao;

    @Override
    public void saveAllOperator(String nid) throws Exception {
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
                    throw new Exception("保存数据异常");
                }
            }
        }
    }

    @Override
    public Integer robotCallAndCalledNum7(String nid, Long applyTime, Integer days) {
        Integer count = 0;
        try {
            Map<String, Object> map = this.getCallAndCalledByDay(nid, applyTime, days);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：" + days + "天内互通时长-手机异常，nid;{},error", nid, e);
        }
        return count;
    }

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

    private void saveOperatorCallDetailByNumber(String operatorNum, String nid) {
        List<JSONObject> operator = mongoHandler.getOperatorCallDetailByNumber(operatorNum);
        if (null != operator && operator.size() > 0) {
            List<OperatorCallRecord> list = new ArrayList<>();
            for (JSONObject json : operator) {
                String peer_number = json.getString("peer_number");
                if (!PhoneUtils.isMobile(peer_number)) {
                    continue;
                }
                OperatorCallRecord record = new OperatorCallRecord();
                record.setUserId(json.getLong("clientId"));
                record.setNid(nid);
                record.setPhone(json.getString("phone"));
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

    private void saveUserContact(String clientNum, String nid) {
        List<JSONObject> client = mongoHandler.getUserContact(clientNum);
        if (null != client && client.size() > 0) {
            List<ClientContact> list = new ArrayList<>();
            for (JSONObject json : client) {
                String contactPhone = PhoneUtils.cleanTel(json.getString("contactsPhone"));
                if (!PhoneUtils.isMobile(contactPhone)) {
                    continue;
                }

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
                contactPhone = contactPhone.length() > 32 ? contactPhone.substring(0, 30) : contactPhone;
                contact.setContactsPhone(contactPhone);
                list.add(contact);
            }

            if (list.size() > 0) {
                clientContactDao.saveBatch(list);
            }
        }
    }
}

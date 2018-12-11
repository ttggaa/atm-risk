package com.risk.controller.service.handler;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.DateTools;
import com.risk.controller.service.common.utils.PhoneUtils;
import com.risk.controller.service.entity.DataOrderMapping;
import com.risk.controller.service.mongo.dao.MongoCollections;
import com.risk.controller.service.mongo.dao.MongoDao;
import com.risk.controller.service.mongo.dao.MongoQuery;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.DataOrderMappingService;
import com.risk.controller.service.service.ThirdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;

@Service
@Slf4j
public class MongoHandler {
    @Resource
    private MongoDao mongoDao;
    @Resource
    private DataOrderMappingService dataOrderMappingService;
    @Resource
    private ThirdService thirdService;

    /**
     * 通过订单号查询通话记录
     *
     * @param request
     * @return
     */
    public List<JSONObject> getUserDeviceCallRecord(DecisionHandleRequest request) {
        if (null != request.getRobotRequestDTO().getCallRecords() && request.getRobotRequestDTO().getCallRecords().size() > 0) {
            return request.getRobotRequestDTO().getCallRecords();
        }

        if (StringUtils.isBlank(request.getRobotRequestDTO().getClientNum())) {
            DataOrderMapping mapping = dataOrderMappingService.getLastOneByNid(request.getNid());
            if (null != mapping) {
                if (StringUtils.isNotBlank(mapping.getClientNum())) {
                    request.getRobotRequestDTO().setClientNum(mapping.getClientNum());
                }
                if (StringUtils.isNotBlank(mapping.getOperatorNum())) {
                    request.getRobotRequestDTO().setOperatorNum(mapping.getOperatorNum());
                }
            }
        }
        List<JSONObject> list = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getRobotRequestDTO().getClientNum())) {
            MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getClientNum(), MongoQuery.MongoQueryBaseType.eq);
            List<MongoQuery> queries = new ArrayList<>();
            queries.add(query);
            list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_DEVICE_CALL_RECORD, null);
        }

        if (null != list && list.size() > 0) {
            request.getRobotRequestDTO().setCallRecords(list);
            return list;
        } else {
            return this.getUserDeviceCallRecordV1(request);
        }
    }

    private List<JSONObject> getUserDeviceCallRecordV1(DecisionHandleRequest request) {
        List<JSONObject> list = new ArrayList<>();
        try {
            if (null != request.getRobotRequestDTO().getCallRecords() && request.getRobotRequestDTO().getCallRecords().size() > 0) {
                return request.getRobotRequestDTO().getCallRecords();
            }
            // 缓存numbser
            if (StringUtils.isBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("orderNo", request.getNid(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                JSONObject json = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_DEVICE_INFO);
                if (null != json && StringUtils.isNotBlank(json.getString("number"))) {
                    request.getRobotRequestDTO().setNumber(json.getString("number"));
                }
            }

            if (StringUtils.isNotBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getNumber(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_DEVICE_CALL_RECORD, null);
            }
        } catch (Exception e) {
            log.error("查询mongo异常：", e);
        }
        if (null != list && list.size() > 0) {
            request.getRobotRequestDTO().setCallRecords(list);
        }
        return list;
    }


    /**
     * 查询用户设备短信信息
     *
     * @param request
     * @return
     */
    public List<JSONObject> getUserDeviceSms(DecisionHandleRequest request) {
        if (null != request.getRobotRequestDTO().getSms() && request.getRobotRequestDTO().getSms().size() > 0) {
            return request.getRobotRequestDTO().getSms();
        }

        if (StringUtils.isBlank(request.getRobotRequestDTO().getClientNum())) {
            DataOrderMapping mapping = dataOrderMappingService.getLastOneByNid(request.getNid());
            if (null != mapping) {
                if (StringUtils.isNotBlank(mapping.getClientNum())) {
                    request.getRobotRequestDTO().setClientNum(mapping.getClientNum());
                }
                if (StringUtils.isNotBlank(mapping.getOperatorNum())) {
                    request.getRobotRequestDTO().setOperatorNum(mapping.getOperatorNum());
                }
            }
        }
        List<JSONObject> list = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getRobotRequestDTO().getClientNum())) {
            MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getClientNum(), MongoQuery.MongoQueryBaseType.eq);
            List<MongoQuery> queries = new ArrayList<>();
            queries.add(query);
            list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_DEVICE_SMS, null);
        }

        if (null != list && list.size() > 0) {
            request.getRobotRequestDTO().setSms(list);
            return list;
        } else {
            return this.getUserDeviceSmsV1(request);
        }
    }

    private List<JSONObject> getUserDeviceSmsV1(DecisionHandleRequest request) {
        List<JSONObject> list = new ArrayList<>();
        try {
            if (null != request.getRobotRequestDTO().getSms() && request.getRobotRequestDTO().getSms().size() > 0) {
                return request.getRobotRequestDTO().getSms();
            }
            // 缓存numbser
            if (StringUtils.isBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("orderNo", request.getNid(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                JSONObject json = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_DEVICE_INFO);
                if (null != json && StringUtils.isNotBlank(json.getString("number"))) {
                    request.getRobotRequestDTO().setNumber(json.getString("number"));
                }
            }

            if (StringUtils.isNotBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getNumber(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_DEVICE_SMS, null);
            }
        } catch (Exception e) {
            log.error("查询mongo异常：", e);
        }
        if (null != list && list.size() > 0) {
            request.getRobotRequestDTO().setSms(list);
        }
        return list;
    }

    /**
     * 查询用户设备通讯录信息
     *
     * @param request
     * @return
     */
    public List<JSONObject> getUserDeviceContact(DecisionHandleRequest request) {
        if (null != request.getRobotRequestDTO().getContacts() && request.getRobotRequestDTO().getContacts().size() > 0) {
            return request.getRobotRequestDTO().getContacts();
        }

        if (StringUtils.isBlank(request.getRobotRequestDTO().getClientNum())) {
            DataOrderMapping mapping = dataOrderMappingService.getLastOneByNid(request.getNid());
            if (null != mapping) {
                if (StringUtils.isNotBlank(mapping.getClientNum())) {
                    request.getRobotRequestDTO().setClientNum(mapping.getClientNum());
                }
                if (StringUtils.isNotBlank(mapping.getOperatorNum())) {
                    request.getRobotRequestDTO().setOperatorNum(mapping.getOperatorNum());
                }
            }
        }
        List<JSONObject> list = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getRobotRequestDTO().getClientNum())) {
            MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getClientNum(), MongoQuery.MongoQueryBaseType.eq);
            List<MongoQuery> queries = new ArrayList<>();
            queries.add(query);
            list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_DEVICE_CONTACT, null);
        }

        if (null != list && list.size() > 0) {
            request.getRobotRequestDTO().setContacts(list);
            return list;
        } else {
            return this.getUserDeviceContactV1(request);
        }
    }

    private List<JSONObject> getUserDeviceContactV1(DecisionHandleRequest request) {
        List<JSONObject> list = new ArrayList<>();
        try {
            if (null != request.getRobotRequestDTO().getContacts() && request.getRobotRequestDTO().getContacts().size() > 0) {
                return request.getRobotRequestDTO().getContacts();
            }
            // 缓存numbser
            if (StringUtils.isBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("orderNo", request.getNid(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                JSONObject json = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_DEVICE_INFO);
                if (null != json && StringUtils.isNotBlank(json.getString("number"))) {
                    request.getRobotRequestDTO().setNumber(json.getString("number"));
                }
            }

            if (StringUtils.isNotBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getNumber(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_DEVICE_CONTACT, null);
            }
        } catch (Exception e) {
            log.error("查询mongo异常：", e);
        }
        if (null != list && list.size() > 0) {
            request.getRobotRequestDTO().setContacts(list);
        }
        return list;
    }

    /**
     * 查询运营商短信
     *
     * @param request
     * @return
     */
    public List<JSONObject> getUserOperatorSms(DecisionHandleRequest request) {

        if (StringUtils.isBlank(request.getRobotRequestDTO().getOperatorNum())) {
            DataOrderMapping mapping = dataOrderMappingService.getLastOneByNid(request.getNid());
            if (null != mapping) {
                if (StringUtils.isNotBlank(mapping.getClientNum())) {
                    request.getRobotRequestDTO().setClientNum(mapping.getClientNum());
                }
                if (StringUtils.isNotBlank(mapping.getOperatorNum())) {
                    request.getRobotRequestDTO().setOperatorNum(mapping.getOperatorNum());
                }
            }
        }
        List<JSONObject> list = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getRobotRequestDTO().getOperatorNum())) {
            MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getOperatorNum(), MongoQuery.MongoQueryBaseType.eq);
            List<MongoQuery> queries = new ArrayList<>();
            queries.add(query);
            list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_OPERATOR_SMS, null);
        }

        if (null != list && list.size() > 0) {
            return list;
        } else {
            return this.getUserOperatorSmsV1(request);
        }
    }

    private List<JSONObject> getUserOperatorSmsV1(DecisionHandleRequest request) {
        List<JSONObject> list = new ArrayList<>();
        try {
            // 缓存numbser
            if (StringUtils.isBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("orderNo", request.getNid(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                JSONObject json = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_DEVICE_INFO);
                if (null != json && StringUtils.isNotBlank(json.getString("number"))) {
                    request.getRobotRequestDTO().setNumber(json.getString("number"));
                }
            }

            if (StringUtils.isNotBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getNumber(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_OPERATOR_SMS, null);
            }
        } catch (Exception e) {
            log.error("查询mongo异常：", e);
        }

        return list;
    }

    /**
     * 查询用户设紧急联系人
     *
     * @param request
     * @return
     */
    public List<JSONObject> getUserMainContact(DecisionHandleRequest request) {
        if (null != request.getRobotRequestDTO().getMainContacts() && request.getRobotRequestDTO().getMainContacts().size() > 0) {
            return request.getRobotRequestDTO().getMainContacts();
        }

        if (StringUtils.isBlank(request.getRobotRequestDTO().getClientNum())) {
            DataOrderMapping mapping = dataOrderMappingService.getLastOneByNid(request.getNid());
            if (null != mapping) {
                if (StringUtils.isNotBlank(mapping.getClientNum())) {
                    request.getRobotRequestDTO().setClientNum(mapping.getClientNum());
                }
                if (StringUtils.isNotBlank(mapping.getOperatorNum())) {
                    request.getRobotRequestDTO().setOperatorNum(mapping.getOperatorNum());
                }
            }
        }
        List<JSONObject> list = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getRobotRequestDTO().getClientNum())) {
            MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getClientNum(), MongoQuery.MongoQueryBaseType.eq);
            List<MongoQuery> queries = new ArrayList<>();
            queries.add(query);
            list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_USER_MAIN_CONTACT, null);
        }

        if (null != list && list.size() > 0) {
            request.getRobotRequestDTO().setMainContacts(list);
            return list;
        } else {
            return this.getUserMainContactV1(request);
        }
    }


    /**
     * 查询mongo数据
     *
     * @param request
     * @return
     */
    private List<JSONObject> getUserMainContactV1(DecisionHandleRequest request) {
        List<JSONObject> list = new ArrayList<>();
        try {
            if (null != request.getRobotRequestDTO().getMainContacts() && request.getRobotRequestDTO().getMainContacts().size() > 0) {
                return request.getRobotRequestDTO().getMainContacts();
            }
            // 缓存numbser
            if (StringUtils.isBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("orderNo", request.getNid(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                JSONObject json = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_DEVICE_INFO);
                if (null != json && StringUtils.isNotBlank(json.getString("number"))) {
                    request.getRobotRequestDTO().setNumber(json.getString("number"));
                }
            }

            if (StringUtils.isNotBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getNumber(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_USER_MAIN_CONTACT, null);
            }
        } catch (Exception e) {
            log.error("查询mongo异常：", e);
        }
        if (null != list && list.size() > 0) {
            request.getRobotRequestDTO().setMainContacts(list);
        }
        return list;
    }

    /**
     * 查询运营商实名信息等基础信息
     *
     * @param request
     * @return
     */
    public JSONObject getOperatorInfo(DecisionHandleRequest request) {
        if (null != request.getRobotRequestDTO().getOperatorInfo()) {
            return request.getRobotRequestDTO().getOperatorInfo();
        }
        if (StringUtils.isBlank(request.getRobotRequestDTO().getOperatorNum())) {
            DataOrderMapping mapping = dataOrderMappingService.getLastOneByNid(request.getNid());
            if (null != mapping) {
                if (StringUtils.isNotBlank(mapping.getClientNum())) {
                    request.getRobotRequestDTO().setClientNum(mapping.getClientNum());
                }
                if (StringUtils.isNotBlank(mapping.getOperatorNum())) {
                    request.getRobotRequestDTO().setOperatorNum(mapping.getOperatorNum());
                }
            }
        }
        JSONObject result = new JSONObject();
        if (StringUtils.isNotBlank(request.getRobotRequestDTO().getOperatorNum())) {
            MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getOperatorNum(), MongoQuery.MongoQueryBaseType.eq);
            List<MongoQuery> queries = new ArrayList<>();
            queries.add(query);
            result = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_OPERATOR_INFO);
        }

        if (null != result && result.size() > 0) {
            request.getRobotRequestDTO().setOperatorInfo(result);
            return result;
        } else {
            return this.getOperatorInfoV1(request);
        }
    }

    private JSONObject getOperatorInfoV1(DecisionHandleRequest request) {
        JSONObject result = new JSONObject();
        try {
            // 缓存numbser
            if (StringUtils.isBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("orderNo", request.getNid(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                JSONObject json = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_DEVICE_INFO);
                if (null != json && StringUtils.isNotBlank(json.getString("number"))) {
                    request.getRobotRequestDTO().setNumber(json.getString("number"));
                }
            }

            if (StringUtils.isNotBlank(request.getRobotRequestDTO().getNumber())) {
                MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getNumber(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                result = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_OPERATOR_INFO);
            }
        } catch (Exception e) {
            log.error("查询mongo异常：", e);
        }
        return result;
    }

    /**
     * 查询运营商通话记录
     *
     * @param request
     * @return
     */
    public List<JSONObject> getUserOperatorCallDetail(DecisionHandleRequest request) {

        if (null != request.getRobotRequestDTO().getOperatorCallRecords() && request.getRobotRequestDTO().getOperatorCallRecords().size() > 0) {
            return request.getRobotRequestDTO().getOperatorCallRecords();
        }
        if (StringUtils.isBlank(request.getRobotRequestDTO().getClientNum())) {
            DataOrderMapping mapping = dataOrderMappingService.getLastOneByNid(request.getNid());
            if (null != mapping) {
                if (StringUtils.isNotBlank(mapping.getClientNum())) {
                    request.getRobotRequestDTO().setClientNum(mapping.getClientNum());
                }
                if (StringUtils.isNotBlank(mapping.getOperatorNum())) {
                    request.getRobotRequestDTO().setOperatorNum(mapping.getOperatorNum());
                }
            }
        }
        List<JSONObject> list = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getRobotRequestDTO().getClientNum())) {
            MongoQuery query = new MongoQuery("number", request.getRobotRequestDTO().getOperatorNum(), MongoQuery.MongoQueryBaseType.eq);
            List<MongoQuery> queries = new ArrayList<>();
            queries.add(query);
            list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_OPERATOR_CALLS_DETAIL, null);
        }

        if (null != list && list.size() > 0) {
            request.getRobotRequestDTO().setOperatorCallRecords(list);
            return list;
        } else {
            this.thirdService.repeatAddOperator(request.getMerchantCode(), request.getNid());
            return null;
        }
    }

    /**
     * 查询树美多头信息
     *
     * @param request
     * @return
     */
    public JSONObject getShumeiMultipoint(DecisionHandleRequest request) {
        JSONObject result = new JSONObject();
        try {
            if (null != request.getRobotRequestDTO().getShuMeiMultipoint()) {
                return request.getRobotRequestDTO().getShuMeiMultipoint();
            }
            if (StringUtils.isNotBlank(request.getNid())) {
                MongoQuery query = new MongoQuery("orders", request.getNid(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                result = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_SHUMEI_BORROWS);
            }
        } catch (Exception e) {
            log.error("查询mongo异常：", e);
        }
        if (null != result) {
            request.getRobotRequestDTO().setShuMeiMultipoint(result);
        }
        return result;
    }

    /**
     * 通过number查询运营商通话记录
     *
     * @param operatorNum
     * @return
     */
    public List<JSONObject> getOperatorCallDetailByNumber(String operatorNum) {
        List<JSONObject> list = new ArrayList<>();
        if (StringUtils.isNotBlank(operatorNum)) {
            MongoQuery query = new MongoQuery("number", operatorNum, MongoQuery.MongoQueryBaseType.eq);
            List<MongoQuery> queries = new ArrayList<>();
            queries.add(query);
            list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_OPERATOR_CALLS_DETAIL, null);
        }
        return list;
    }

    /**
     * 通过clientNum查询用户设备通讯录
     *
     * @param clientNum
     * @return
     */
    public List<JSONObject> getUserContact(String clientNum) {
        List<JSONObject> list = new ArrayList<>();
        if (StringUtils.isNotBlank(clientNum)) {
            MongoQuery query = new MongoQuery("number", clientNum, MongoQuery.MongoQueryBaseType.eq);
            List<MongoQuery> queries = new ArrayList<>();
            queries.add(query);
            list = mongoDao.find(queries, JSONObject.class, MongoCollections.DB_DEVICE_CONTACT, null);
        }
        return list;
    }

    /**
     * 运营商互通。查询最近ruleOldDays天互相通话次数
     *
     * @param ruleDays    订单申请时间
     * @param ruleDays    最近多少天
     * @param callDetails 运营商通话记录
     * @return 互通次数
     */
    public int getOptEachCallNum(Long applyTime, int ruleDays, List<JSONObject> callDetails) {
        int count = 0;

        if (CollectionUtils.isEmpty(callDetails)) {
            return count;
        }

        // 运营商主叫被叫，总次数
        Map<String, Integer> eachCallNum = new HashMap<>();//所有通讯录主叫次数
        Map<String, Integer> eachCalledNum = new HashMap<>();//所有通讯录被叫次数

        for (JSONObject jsonObject : callDetails) {
            String strTime = jsonObject.getString("time");// 通话时间
            String peerNumber = jsonObject.getString("peer_number");//通话手机号码
            Integer duration = jsonObject.getInteger("duration");//通话时长
            String dialType = jsonObject.getString("dial_type");//DIALED被叫，DIAL主叫

            if (!PhoneUtils.isMobile(peerNumber) || StringUtils.isBlank(strTime) || null == duration || duration == 0) {
                continue;
            }
            Long diffDays = Math.abs(DateTools.getDayDiff(new Date(applyTime), DateTools.convert(strTime)));
            if (diffDays.compareTo(Long.valueOf(ruleDays)) <= 0) {
                // 运营商主叫被叫
                if ("DIALED".equals(dialType)) {
                    if (eachCalledNum.containsKey(peerNumber)) {
                        eachCalledNum.put(peerNumber, eachCalledNum.get(peerNumber) + 1);
                    } else {
                        eachCalledNum.put(peerNumber, 1);
                    }
                } else if ("DIAL".equals(dialType)) {
                    if (eachCallNum.containsKey(peerNumber)) {
                        eachCallNum.put(peerNumber, eachCallNum.get(peerNumber) + 1);
                    } else {
                        eachCallNum.put(peerNumber, 1);
                    }
                }
            }
        }

        // 计算通讯录互通
        for (Map.Entry<String, Integer> entry : eachCallNum.entrySet()) {
            // 该手机号码既有主叫也有被叫
            if (eachCalledNum.containsKey(entry.getKey())) {
                count += (entry.getValue() + eachCalledNum.get(entry.getKey())) / 2;
            }
        }
        return count;
    }

    /**
     * 通讯录ruleDays之内的互相通话次数
     *
     * @param applyTime   申请时间
     * @param ruleDays    天数
     * @param contactList 设备通讯录
     * @param callDetails 运营商通话记录
     * @return
     */
    public int getCntEachCallNum(Long applyTime, Integer ruleDays, List<JSONObject> contactList, List<JSONObject> callDetails) {
        int count = 0;

        if (CollectionUtils.isEmpty(callDetails) && CollectionUtils.isEmpty(contactList)) {
            return count;
        }
        Set<String> devicePhones = new HashSet<>();
        contactList.forEach(json -> {
            String phone = json.getString("contactsPhone");
            phone = PhoneUtils.cleanTel(phone);
            if (PhoneUtils.isMobile(phone)) {
                devicePhones.add(phone);
            }
        });


        // 运营商主叫被叫，总次数
        Map<String, Integer> eachCallNum = new HashMap<>();//所有通讯录主叫次数
        Map<String, Integer> eachCalledNum = new HashMap<>();//所有通讯录被叫次数

        for (JSONObject jsonObject : callDetails) {
            String strTime = jsonObject.getString("time");// 通话时间
            String peerNumber = jsonObject.getString("peer_number");//通话手机号码
            Integer duration = jsonObject.getInteger("duration");//通话时长
            String dialType = jsonObject.getString("dial_type");//DIALED被叫，DIAL主叫

            if (!PhoneUtils.isMobile(peerNumber) || StringUtils.isBlank(strTime) || null == duration || duration == 0) {
                continue;
            }
            Long diffDays = Math.abs(DateTools.getDayDiff(new Date(applyTime), DateTools.convert(strTime)));

            // 通讯录互通
            if (diffDays.compareTo(Long.valueOf(ruleDays)) <= 0 && devicePhones.contains(peerNumber)) {
                // 运营商主叫被叫
                if ("DIALED".equals(dialType)) {
                    if (eachCalledNum.containsKey(peerNumber)) {
                        eachCalledNum.put(peerNumber, eachCalledNum.get(peerNumber) + 1);
                    } else {
                        eachCalledNum.put(peerNumber, 1);
                    }
                } else if ("DIAL".equals(dialType)) {
                    if (eachCallNum.containsKey(peerNumber)) {
                        eachCallNum.put(peerNumber, eachCallNum.get(peerNumber) + 1);
                    } else {
                        eachCallNum.put(peerNumber, 1);
                    }
                }
            }
        }

        // 计算通讯录互通
        for (Map.Entry<String, Integer> entry : eachCallNum.entrySet()) {
            // 该手机号码既有主叫也有被叫
            if (eachCalledNum.containsKey(entry.getKey())) {
                count += (entry.getValue() + eachCalledNum.get(entry.getKey())) / 2;
            }
        }
        return count;
    }
}

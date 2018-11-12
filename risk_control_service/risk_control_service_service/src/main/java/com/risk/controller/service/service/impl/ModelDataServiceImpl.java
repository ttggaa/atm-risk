package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.DateConvert;
import com.risk.controller.service.common.utils.DateTools;
import com.risk.controller.service.common.utils.IdcardUtils;
import com.risk.controller.service.common.utils.PhoneUtils;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.handler.MongoHandler;
import com.risk.controller.service.mongo.dao.MongoCollections;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.DataOrderMappingService;
import com.risk.controller.service.service.ModelDataService;
import com.risk.controller.service.service.PaixuService;
import com.risk.controller.service.service.ThirdService;
import com.risk.controller.service.utils.Average;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;


/**
 * 保存模型数据的service
 */
@Slf4j
@Service
public class ModelDataServiceImpl implements ModelDataService {

    @Autowired
    private LocalCache localCache;
    @Autowired
    private ThirdService thirdService;
    @Autowired
    private MongoHandler mongoHandler;
    @Autowired
    private StaOperatorCallsDao staOperatorCallsDao;
    @Autowired
    private StaUserBaseinfoDao staUserBaseinfoDao;
    @Autowired
    private RiskModelOperatorReportDao riskModelOperatorReportDao;
    @Autowired
    private DataOrderMappingService dataOrderMappingService;
    @Autowired
    private PaixuService paixuService;
    @Autowired
    private StaSmBorrowsDao staSmBorrowsDao;
    @Autowired
    private RobotResultDetailDao robotResultDetailDao;
    @Autowired
    private DecisionReqLogDao decisionReqLogDao;

    @Override
    @Async
    public void saveDataBySql(String sql) {
        if (StringUtils.isBlank(sql)) {
            return;
        }
        List<Map<String, Object>> list = robotResultDetailDao.runModelBySql(sql);
        if (null != list && list.size() > 0) {
            for (Map<String, Object> map : list) {
                Object nidObject = map.get("nid");
                try {
                    if (null != nidObject) {
                        String nid = (String) nidObject;
                        DecisionReqLog reqLog = decisionReqLogDao.getbyNid(nid);
                        if (null != reqLog) {
                            DecisionHandleRequest request = JSONObject.parseObject(reqLog.getReqData(), DecisionHandleRequest.class);
                            this.saveData(request);
                        }
                    }
                } catch (Exception e) {
                    log.error("批量跑数据异常：nid:{},error:{}", nidObject, e);
                }
            }
        }
    }

    /**
     * 入口方法
     *
     * @param request
     */
    @Override
    public void saveData(DecisionHandleRequest request) throws Exception {
        this.saveOperatorCalls(request);
        this.saveSmBorrow(request);
        // 运营商报告相关
        this.genBasicCheckItem(request);
        this.genCallFamilyDetail(request);
        this.genCallMidnight(request);
        this.genCallRiskAnalysis(request);
        this.genCallSilentAreas(request);
        this.genUserInfoCheck(request);
    }

    @Override
    public StaUserBaseinfo getUserBaseInfo(DecisionHandleRequest request) {
        if (null == request || StringUtils.isBlank(request.getNid())) {
            return null;
        }
        if (null != request.getRobotRequestDTO().getStaUserBaseinfo()) {
            return request.getRobotRequestDTO().getStaUserBaseinfo();
        }
        StaUserBaseinfo baseinfo = staUserBaseinfoDao.getByNid(request.getNid());
        request.getRobotRequestDTO().setStaUserBaseinfo(baseinfo);
        return baseinfo;
    }

    @Override
    public List<StaOperatorCalls> getOperatorCalls(DecisionHandleRequest request) {
        if (null == request || StringUtils.isBlank(request.getNid())) {
            return null;
        }
        if (null != request.getRobotRequestDTO().getListOperatorCalls() && request.getRobotRequestDTO().getListOperatorCalls().size() > 0) {
            return request.getRobotRequestDTO().getListOperatorCalls();
        }
        List<StaOperatorCalls> list = staOperatorCallsDao.getByNid(request.getNid());
        request.getRobotRequestDTO().setListOperatorCalls(list);
        return list;
    }

    @Override
    public StaSmBorrows getStaSmBorrows(DecisionHandleRequest request) {
        if (null == request || StringUtils.isBlank(request.getNid())) {
            return null;
        }
        if (null != request.getRobotRequestDTO().getStaSmBorrows()) {
            return request.getRobotRequestDTO().getStaSmBorrows();
        }
        StaSmBorrows staSmBorrows = staSmBorrowsDao.getByNid(request.getNid());
        request.getRobotRequestDTO().setStaSmBorrows(staSmBorrows);
        return staSmBorrows;
    }

    /**
     * 保存运营商通话记录
     *
     * @param request
     * @throws Exception
     */
    private void saveOperatorCalls(DecisionHandleRequest request) throws Exception {
        List<JSONObject> callDetails = mongoHandler.getUserOperatorCallDetail(request);
        List<JSONObject> smsDetails = mongoHandler.getUserOperatorSms(request);
        List<JSONObject> contactDetails = mongoHandler.getUserDeviceContact(request);
        JSONObject operatorInfo = mongoHandler.getOperatorInfo(request);
        if (null == operatorInfo) {
            throw new Exception("operatorInfo为空");
        }
        if (null == callDetails || callDetails.size() <= 0) {
            throw new Exception("callDetails为空");
        }
        if (null == contactDetails || contactDetails.size() <= 0) {
            throw new Exception("ocontactDetails为空");
        }

        StaUserBaseinfo userBaseinfo = this.getStaUserBaseInfo(request, callDetails, contactDetails, operatorInfo);
        List<StaOperatorCalls> operatorCalls = this.getStaStaOperatorCalls(request, callDetails, smsDetails, contactDetails);
        staUserBaseinfoDao.saveOrUpdate(userBaseinfo);
        if (null != operatorCalls && operatorCalls.size() > 0) {
            this.staOperatorCallsDao.saveOrUpdateBatch(operatorCalls);
        }
    }

    /**
     * 保存运营商通话记录信息
     *
     * @param request
     * @param callDetails
     * @param smsDetails
     * @param contactDetails
     * @return
     */
    private List<StaOperatorCalls> getStaStaOperatorCalls(DecisionHandleRequest request,
                                                          List<JSONObject> callDetails,
                                                          List<JSONObject> smsDetails,
                                                          List<JSONObject> contactDetails) {
        List<StaOperatorCalls> list = new ArrayList<>();
        String callDays = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.SYSTEMCFG, "robot.operator.call.days");
        if (StringUtils.isNotBlank(callDays)) {
            String[] days = callDays.split(",");
            for (String ruleDays : days) {
                StaOperatorCalls operatorCalls = new StaOperatorCalls();
                operatorCalls.setNid(request.getNid());
                operatorCalls.setDay(Integer.valueOf(ruleDays));

                // 通讯录
                Set<String> contactSet = new HashSet<>();
                contactDetails.forEach(jsonObject -> {
                    String phone = jsonObject.getString("contactsPhone");
                    phone = PhoneUtils.cleanTel(phone);
                    if (PhoneUtils.isMobile(phone)) {
                        contactSet.add(phone);
                    }
                });


                // 运营商主叫被叫，总次数
                Set<String> optCallMan = new HashSet<>();
                Set<String> optCalledMan = new HashSet<>();
                Set<String> cntCallMan = new HashSet<>();
                Set<String> cntCalledMan = new HashSet<>();

                Map<String, Integer> eachCallNum = new HashMap<>();//所有通讯录主叫次数
                Map<String, Integer> eachCalledNum = new HashMap<>();//所有通讯录被叫次数
                Map<String, Integer> eachCallTime = new HashMap<>();//所有通讯录主叫+被叫时长

                for (JSONObject jsonObject : callDetails) {
                    String strTime = jsonObject.getString("time");// 通话时间
                    String peerNumber = jsonObject.getString("peer_number");//通话手机号码
                    Integer duration = jsonObject.getInteger("duration");//通话时长
                    String dialType = jsonObject.getString("dial_type");//DIALED被叫，DIAL主叫

                    if (!PhoneUtils.isMobile(peerNumber) || StringUtils.isBlank(strTime) || null == duration || duration == 0) {
                        continue;
                    }
                    Long diffDays = Math.abs(DateTools.getDayDiff(new Date(request.getApplyTime()), DateTools.convert(strTime)));
                    if (diffDays.compareTo(Long.valueOf(ruleDays)) <= 0) {

                        // 运营商主叫被叫
                        if ("DIALED".equals(dialType)) {
                            operatorCalls.setOptCalledNum(operatorCalls.getOptCalledNum() + 1);
                            operatorCalls.setOptCalledTime(operatorCalls.getOptCalledTime() + duration);
                            optCalledMan.add(peerNumber);
                        } else if ("DIAL".equals(dialType)) {
                            operatorCalls.setOptCallNum(operatorCalls.getOptCallNum() + 1);
                            operatorCalls.setOptCallTime(operatorCalls.getOptCallTime() + duration);
                            optCallMan.add(peerNumber);
                        }

                        // 通讯录主叫被叫
                        if (contactSet.contains(peerNumber)) {
                            if ("DIALED".equals(dialType)) {
                                operatorCalls.setCntCalledNum(operatorCalls.getCntCalledNum() + 1);
                                operatorCalls.setCntCalledTime(operatorCalls.getCntCalledTime() + duration);
                                cntCalledMan.add(peerNumber);
                                // 互通
                                if (eachCalledNum.containsKey(peerNumber)) {
                                    eachCalledNum.put(peerNumber, eachCalledNum.get(peerNumber) + 1);
                                    eachCallTime.put(peerNumber, eachCallTime.get(peerNumber) + duration);
                                } else {
                                    eachCalledNum.put(peerNumber, 1);
                                    eachCallTime.put(peerNumber, duration);
                                }
                            } else if ("DIAL".equals(dialType)) {
                                operatorCalls.setCntCallNum(operatorCalls.getCntCallNum() + 1);
                                operatorCalls.setCntCallTime(operatorCalls.getCntCallTime() + duration);
                                cntCallMan.add(peerNumber);

                                // 互通
                                if (eachCallNum.containsKey(peerNumber)) {
                                    eachCallNum.put(peerNumber, eachCallNum.get(peerNumber) + 1);
                                    eachCallTime.put(peerNumber, eachCallTime.get(peerNumber) + duration);
                                } else {
                                    eachCallNum.put(peerNumber, 1);
                                    eachCallTime.put(peerNumber, duration);
                                }
                            }
                        }
                    }
                }

                // 计算运营商总的主叫被叫之和
                operatorCalls.setOptCallManNum(optCallMan.size());
                operatorCalls.setOptCalledManNum(optCalledMan.size());
                optCallMan.addAll(optCalledMan); // 运营商通话人次（主叫+被叫）-手机号码
                operatorCalls.setOptManNum(optCallMan.size());

                // 计算有效通话
                operatorCalls.setCntCallManNum(cntCallMan.size());
                operatorCalls.setCntCalledManNum(cntCalledMan.size());
                operatorCalls.setCntValidNum(operatorCalls.getCntCallNum() + operatorCalls.getCntCalledNum());
                operatorCalls.setCntValidTime(operatorCalls.getCntCallTime() + operatorCalls.getCntCalledTime());
                cntCallMan.addAll(cntCalledMan);// 通讯录通话人次（主叫+被叫）-手机号码
                operatorCalls.setCntValidManNum(cntCallMan.size());

                // 计算通讯录互通
                int eachNum = 0;
                int eachTime = 0;
                int eachManNum = 0;
                for (Map.Entry<String, Integer> entry : eachCallNum.entrySet()) {
                    // 该手机号码既有主叫也有被叫
                    if (eachCalledNum.containsKey(entry.getKey())) {
                        eachNum += (entry.getValue() + eachCalledNum.get(entry.getKey())) / 2;
                        eachTime += eachCallTime.get(entry.getKey());
                        eachManNum++;
                    }
                }
                operatorCalls.setCntEachOtherNum(eachNum);
                operatorCalls.setCntEachOtherTime(eachTime);
                operatorCalls.setCntEachOtherManNum(eachManNum);

                // 运营商短信
                Set<String> optSmsManNum = new HashSet<>();
                for (JSONObject smsDetail : smsDetails) {
                    String strTime = smsDetail.getString("time");// 通话时间
                    String peerNumber = smsDetail.getString("peer_number");//通话手机号码
                    String sendType = smsDetail.getString("send_type");//RECEIVE被叫，SEND主叫

                    if (!PhoneUtils.isMobile(peerNumber) || StringUtils.isBlank(strTime) || StringUtils.isBlank(sendType)) {
                        continue;
                    }

                    Long diffDays = Math.abs(DateTools.getDayDiff(new Date(request.getApplyTime()), DateTools.convert(strTime)));
                    if (diffDays.compareTo(Long.valueOf(ruleDays)) <= 0) {
                        if ("RECEIVE".equals(sendType)) {
                            operatorCalls.setOptSmsReceiveNum(operatorCalls.getOptSmsReceiveNum() + 1);
                            optSmsManNum.add(peerNumber);
                        } else if ("SEND".equals(sendType)) {
                            operatorCalls.setOptSmsSendNum(operatorCalls.getOptSmsSendNum() + 1);
                            optSmsManNum.add(peerNumber);
                        }
                    }
                }
                operatorCalls.setOptSmsManNum(optSmsManNum.size());

                list.add(operatorCalls);
            }
        }
        return list;
    }

    /**
     * 计算用户运营商、设备等基础信息
     *
     * @param request
     * @param callDetails
     * @param contactDetails
     * @param operatorInfo
     * @return
     */
    private StaUserBaseinfo getStaUserBaseInfo(DecisionHandleRequest request,
                                               List<JSONObject> callDetails,
                                               List<JSONObject> contactDetails,
                                               JSONObject operatorInfo) {

        StaUserBaseinfo userBaseinfo = new StaUserBaseinfo();
        userBaseinfo.setNid(request.getNid());
        userBaseinfo.setAge(IdcardUtils.getAgeByIdCard(request.getCardId()));

        String openTimeStr = operatorInfo.getString("open_time");
        int monthCount = 0;
        if (StringUtils.isBlank(openTimeStr)) {
            monthCount = 6;// 没有入网 时间，默认6个月，没有6个月，进不了模型
        } else {
            Date openTime = DateTools.convert(openTimeStr);
            monthCount = DateConvert.getMonthDiff(new Date(), openTime);
        }

        userBaseinfo.setInTime(openTimeStr);
        userBaseinfo.setDuration(monthCount);
        Integer avgFee = 0;
        BigDecimal averageFare = null == operatorInfo.get("averageFare") ? BigDecimal.ZERO : operatorInfo.getBigDecimal("averageFare");
        avgFee = averageFare.multiply(new BigDecimal(100)).intValue();
        userBaseinfo.setOptAvgFee(avgFee);

        // 设备通讯录去重
        Set<String> contactSet = new HashSet<>();
        contactDetails.forEach(jsonObject -> {
            String phone = jsonObject.getString("contactsPhone");
            phone = PhoneUtils.cleanTel(phone);
            if (PhoneUtils.isMobile(phone)) {
                contactSet.add(phone);
            }
        });
        userBaseinfo.setCntNum(contactSet.size());
        // 设置通讯录注册人数
        if (null != request && null != request.getRobotRequestDTO().getUserDeviceContactRegisterCount()) {
            userBaseinfo.setCntRegisterNum(request.getRobotRequestDTO().getUserDeviceContactRegisterCount());
        } else {
            JSONObject rs = this.thirdService.getRegisterCount(contactSet);
            if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
                userBaseinfo.setCntRegisterNum(0);
            } else {
                userBaseinfo.setCntRegisterNum(rs.getInteger("data"));
            }
        }

        Set<String> callShortSet = new HashSet<>(); //保存运营商短号
        Set<String> callSet = new HashSet<>();//保存运营商手机号码
        callDetails.forEach(jsonObject -> {
            String peerNumber = jsonObject.getString("peer_number");//通话手机号码
            if (StringUtils.isNotBlank(peerNumber) && peerNumber.length() == 3 && !StaUserBaseinfo.shortNumSet.contains(peerNumber)) {
                callShortSet.add(peerNumber);
            }
            if (PhoneUtils.isMobile(peerNumber)) {
                callSet.add(peerNumber);
            }
        });

        userBaseinfo.setOptShortNum(callShortSet.size());
        JSONObject rs = this.thirdService.getRegisterCount(callSet);
        if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
            userBaseinfo.setOptCallsRegisterNum(0);
        } else {
            userBaseinfo.setOptCallsRegisterNum(rs.getInteger("data"));
        }

        if (null != request && null != request.getRobotRequestDTO().getDeviceUsedCount()) {
            userBaseinfo.setUserDeviceUsedNum(request.getRobotRequestDTO().getDeviceUsedCount());
        } else {
            rs = this.thirdService.getDeviceUsedCount(request.getUserId());
            if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
                userBaseinfo.setUserDeviceUsedNum(0);
            } else {
                userBaseinfo.setUserDeviceUsedNum(rs.getInteger("data"));
            }
        }

        if (null != request && null != request.getRobotRequestDTO().getUserDeviceCount()) {
            userBaseinfo.setUserDeviceNum(request.getRobotRequestDTO().getUserDeviceCount());
        } else {
            rs = this.thirdService.getDeviceCount(request.getUserId());
            if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
                userBaseinfo.setUserDeviceNum(0);
            } else {
                userBaseinfo.setUserDeviceNum(rs.getInteger("data"));
            }
        }
        return userBaseinfo;
    }

    /**
     * 获取运营商报告信息
     *
     * @param request
     * @return
     */
    @Override
    public JSONObject getOperatorReport(DecisionHandleRequest request) {
        JSONObject report = null;

        if (null != request.getRobotRequestDTO().getOperatorReport()) {
            return request.getRobotRequestDTO().getOperatorReport();
        }

        DataOrderMapping dataOrderMapping = dataOrderMappingService.getLastOneByNid(request.getNid());
        Map<String, Object> queryMap = new HashMap<String, Object>();
        queryMap.put("number", dataOrderMapping.getOperatorNum());

        List<JSONObject> rawReport = paixuService.getEqMongoData(queryMap, MongoCollections.DB_OPERATOR_MOJIE_INFO);
        if (null != rawReport && rawReport.size() > 0) {
            JSONObject object = (JSONObject) rawReport.get(0);
            report = JSON.parseObject(object.getString("clientReport"), JSONObject.class);
            request.getRobotRequestDTO().setOperatorReport(report);
        } else {
            return null;
        }

        return report;
    }

    /**
     * 风险通话分析保存
     *
     * @param request
     */
    public void genCallRiskAnalysis(DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);
        JSONArray riskCalls = operatorReport.getJSONArray("call_risk_analysis");
        List<Map> listParams = new ArrayList();
        riskCalls.forEach(call -> {
            JSONObject call_risk_analysis = (JSONObject) call;
            JSONObject analysis_point = ((JSONObject) call).getJSONObject("analysis_point");
            JSONObject call_analysis_dial_point = analysis_point.getJSONObject("call_analysis_dial_point");
            JSONObject call_analysis_dialed_point = analysis_point.getJSONObject("call_analysis_dialed_point");

            Map params = new HashMap();
            params.put("nid", request.getNid());
            params.put("phone", request.getUserName());
            params.put("analysis_item", call_risk_analysis.getString("analysis_item"));
            params.put("analysis_desc", call_risk_analysis.getString("analysis_desc"));

            params.put("avg_call_time_6m", analysis_point.getString("avg_call_time_6m"));
            params.put("call_cnt_1m", analysis_point.getString("call_cnt_1m"));
            params.put("call_cnt_3m", analysis_point.getString("call_cnt_3m"));
            params.put("call_cnt_6m", analysis_point.getString("call_cnt_6m"));
            params.put("call_time_3m", analysis_point.getString("call_time_3m"));
            params.put("avg_call_cnt_6m", analysis_point.getString("avg_call_cnt_6m"));
            params.put("call_time_6m", analysis_point.getString("call_time_6m"));
            params.put("avg_call_cnt_3m", analysis_point.getString("avg_call_cnt_3m"));
            params.put("call_time_1m", analysis_point.getString("call_time_1m"));
            params.put("avg_call_time_3m", analysis_point.getString("avg_call_time_3m"));

            params.put("call_dial_time_6m", call_analysis_dial_point.getString("call_dial_time_6m"));
            params.put("call_dial_time_3m", call_analysis_dial_point.getString("call_dial_time_3m"));
            params.put("avg_call_dial_cnt_6m", call_analysis_dial_point.getString("avg_call_dial_cnt_6m"));
            params.put("call_dial_cnt_1m", call_analysis_dial_point.getString("call_dial_cnt_1m"));
            params.put("avg_call_dial_cnt_3m", call_analysis_dial_point.getString("avg_call_dial_cnt_3m"));
            params.put("avg_call_dial_time_6m", call_analysis_dial_point.getString("avg_call_dial_time_6m"));
            params.put("call_dial_cnt_3m", call_analysis_dial_point.getString("call_dial_cnt_3m"));
            params.put("avg_call_dial_time_3m", call_analysis_dial_point.getString("avg_call_dial_time_3m"));
            params.put("call_dial_time_1m", call_analysis_dial_point.getString("call_dial_time_1m"));
            params.put("call_dial_cnt_6m", call_analysis_dial_point.getString("call_dial_cnt_6m"));

            params.put("call_dialed_time_6m", call_analysis_dialed_point.getString("call_dialed_time_6m"));
            params.put("call_dialed_time_3m", call_analysis_dialed_point.getString("call_dialed_time_3m"));
            params.put("avg_call_dialed_cnt_6m", call_analysis_dialed_point.getString("avg_call_dialed_cnt_6m"));
            params.put("call_dialed_cnt_1m", call_analysis_dialed_point.getString("call_dialed_cnt_1m"));
            params.put("avg_call_dialed_cnt_3m", call_analysis_dialed_point.getString("avg_call_dialed_cnt_3m"));
            params.put("avg_call_dialed_time_6m", call_analysis_dialed_point.getString("avg_call_dialed_time_6m"));
            params.put("call_dialed_cnt_3m", call_analysis_dialed_point.getString("call_dialed_cnt_3m"));
            params.put("avg_call_dialed_time_3m", call_analysis_dialed_point.getString("avg_call_dialed_time_3m"));
            params.put("call_dialed_time_1m", call_analysis_dialed_point.getString("call_dialed_time_1m"));
            params.put("call_dialed_cnt_6m", call_analysis_dialed_point.getString("call_dialed_cnt_6m"));
            Date date = new Date();
            params.put("add_time", date);
            params.put("update_time", date);

            listParams.add(params);
        });

        try {
            riskModelOperatorReportDao.saveCallRiskAnalysis(listParams);
        } catch (Exception e) {
            log.error("[模型数据-生成]：genCallRiskAnalysis插入数据出错,nid:{}", request.getNid());
            e.printStackTrace();
        }
    }

    /**
     * 信息校验数据保存
     *
     * @param request
     */
    public void genBasicCheckItem(DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);

        JSONArray basic_check_items = operatorReport.getJSONArray("basic_check_items");
        Map params = new HashMap();
        params.put("nid", request.getNid());
        params.put("phone", request.getUserName());
        Date date = new Date();
        params.put("add_time", date);
        params.put("update_time", date);

        basic_check_items.forEach(call -> {
            JSONObject item = (JSONObject) call;

            String check_item = item.getString("check_item") == null ? "" : item.getString("check_item").equalsIgnoreCase("null") ? "" : item.getString("check_item");
            if (StringUtils.isEmpty(check_item)) {
                return;
            }

            String comment = item.getString("comment") == null ? "" : item.getString("comment").equalsIgnoreCase("null") ? "" : item.getString("comment");
            switch (check_item) {
                case "idcard_check":
                    params.put("idcard_check", item.getString("result"));
                    params.put("idcard_check_comment", comment);
                    break;
                case "email_check":
                    params.put("email_check", item.getString("result"));
                    params.put("email_check_comment", comment);
                    break;
                case "address_check":
                    params.put("address_check", item.getString("result"));
                    params.put("address_check_comment", comment);
                    break;
                case "call_data_check":
                    params.put("call_data_check", item.getString("result"));
                    params.put("call_data_check_comment", comment);
                    break;
                case "idcard_match":
                    params.put("idcard_match", item.getString("result"));
                    params.put("idcard_match_comment", comment);
                    break;
                case "name_match":
                    params.put("name_match", item.getString("result"));
                    params.put("name_match_comment", comment);
                    break;
                case "is_name_and_idcard_in_court_black":
                    params.put("is_name_and_idcard_in_court_black", item.getString("result"));
                    params.put("is_name_and_idcard_in_court_black_comment", comment);
                    break;
                case "is_name_and_idcard_in_finance_black":
                    params.put("is_name_and_idcard_in_finance_black", item.getString("result"));
                    params.put("is_name_and_idcard_in_finance_black_comment", comment);
                    break;
                case "is_name_and_mobile_in_finance_black":
                    params.put("is_name_and_mobile_in_finance_black", item.getString("result"));
                    params.put("is_name_and_mobile_in_finance_black_comment", comment);
                    break;
                case "mobile_silence_3m":
                    params.put("mobile_silence_3m", item.getString("result"));
                    params.put("mobile_silence_3m_comment", comment);
                    break;
                case "mobile_silence_6m":
                    params.put("mobile_silence_6m", item.getString("result"));
                    params.put("mobile_silence_6m_comment", comment);
                    break;
                case "arrearage_risk_3m":
                    params.put("arrearage_risk_3m", item.getString("result"));
                    params.put("arrearage_risk_3m_comment", comment);
                    break;
                case "arrearage_risk_6m":
                    params.put("arrearage_risk_6m", item.getString("result"));
                    params.put("arrearage_risk_6m_comment", comment);
                    break;
                case "binding_risk":
                    params.put("binding_risk", item.getString("result"));
                    params.put("binding_risk_comment", comment);
                    break;
                default:
                    return;
            }
        });
        try {
            riskModelOperatorReportDao.saveBasicCheckItem(params);
        } catch (Exception e) {
            log.error("[模型数据-生成]：genBasicCheckItem插入数据出错,nid:{}", request.getNid());
            e.printStackTrace();
        }
    }

    /**
     * 用户信息监测
     *
     * @param request
     */
    public void genUserInfoCheck(DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);

        JSONArray user_info_check = operatorReport.getJSONArray("user_info_check");
        user_info_check.forEach(call -> {
            JSONObject item = (JSONObject) call;
            if (null == item || null == item.getJSONObject("check_black_info")) {
                return;
            }

            JSONObject check_black_info = item.getJSONObject("check_black_info");
            check_black_info.put("nid", request.getNid());
            check_black_info.put("phone", request.getUserName());

            JSONObject check_search_info = item.getJSONObject("check_search_info");
            Set<String> searchInfoKeys = check_search_info.keySet();
            searchInfoKeys.forEach(key -> {
                check_black_info.put(key, check_search_info.get(key));
                if (key.equalsIgnoreCase("idcard_with_other_names")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
                if (key.equalsIgnoreCase("idcard_with_other_phones")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
                if (key.equalsIgnoreCase("phone_with_other_names")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
                if (key.equalsIgnoreCase("phone_with_other_idcards")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
                if (key.equalsIgnoreCase("arised_open_web")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
            });
            Date currDate = new Date();
            check_black_info.put("add_time", currDate);
            check_black_info.put("update_time", currDate);
            try {
                riskModelOperatorReportDao.saveCheckBlackInfo(check_black_info);
            } catch (Exception e) {
                log.error("[模型数据-生成]：genCheckBlackInfo插入数据出错,nid:{}", request.getNid());
                e.printStackTrace();
            }
        });
    }

    /**
     * 亲情网相关
     *
     * @param request
     */
    public void genCallFamilyDetail(DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);
        List<JSONObject> listParams = new ArrayList();

        JSONArray call_family_detail = operatorReport.getJSONArray("call_family_detail");
        call_family_detail.forEach(call -> {
            JSONObject detail = (JSONObject) call;
            JSONObject item = detail.getJSONObject("item");
            item.put("nid", request.getNid());
            item.put("phone", request.getUserName());
            item.put("app_point", detail.getString("app_point"));
            item.put("app_point_zh", detail.getString("app_point_zh"));
            Date date = new Date();
            item.put("add_time", date);
            item.put("update_time", date);

            listParams.add(item);
        });

        try {
            riskModelOperatorReportDao.saveCallFamilyDetail(listParams);
        } catch (Exception e) {
            log.error("[模型数据-生成]：genCallFamilyDetail插入数据出错,nid:{}", request.getNid());
            e.printStackTrace();
        }
    }

    /**
     * 通话时段分析-深夜通话
     *
     * @param request
     */
    public void genCallMidnight(DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);
        JSONArray call_duration_detail = operatorReport.getJSONArray("call_duration_detail");
        call_duration_detail.forEach(call -> {
            JSONObject detail = (JSONObject) call;
            if (detail.getString("key").equalsIgnoreCase("call_duration_detail_3m")) {
                JSONArray duration_list = detail.getJSONArray("duration_list");
                duration_list.forEach(item -> {
                    JSONObject itemJson = (JSONObject) item;
                    if (itemJson.getString("time_step").equals("midnight")) {
                        itemJson = itemJson.getJSONObject("item");
                        itemJson.put("nid", request.getNid());
                        itemJson.put("phone", request.getUserName());
                        itemJson.put("time_step", "midnight");
                        itemJson.put("time_step_zh", "深夜[1:30-5:30]");
                        Date date = new Date();
                        itemJson.put("add_time", date);
                        itemJson.put("update_time", date);
                        try {
                            riskModelOperatorReportDao.saveCallMidnight(itemJson);
                        } catch (Exception e) {
                            log.error("[模型数据-生成]：genCallMidnight插入数据出错,nid:{}", request.getNid());
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * 手机静默-联系人-出行
     *
     * @param request
     */
    public void genCallSilentAreas(DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);

        JSONObject params = new JSONObject();
        params.put("nid", request.getNid());
        params.put("phone", request.getUserName());
        Date date = new Date();
        params.put("add_time", date);
        params.put("update_time", date);
        JSONArray behaviorCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.BEHAVIOR_CHECK.getValue());
        // 手机静默情况
        for (Object item : behaviorCheck) {
            JSONObject itemJson = (JSONObject) item;
            if (StringUtils.isNotEmpty(itemJson.getString("check_point"))
                    && !itemJson.getString("check_point").equalsIgnoreCase("phone_silent")) {
                params.put("phone_silent", itemJson.getInteger("score"));
            }
        }
        // 出行
        JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.TRIP_INFO.getValue());
        Set<String> areas = new HashSet<>();
        for (Object item : tripInfo) {
            JSONObject itemJson = (JSONObject) item;
            if (StringUtils.isNotEmpty(itemJson.getString("trip_dest"))
                    && !itemJson.getString("trip_dest").equalsIgnoreCase("null")) {
                areas.add(itemJson.getString("trip_dest"));
            }
            if (StringUtils.isNotEmpty(itemJson.getString("trip_leave"))
                    && !itemJson.getString("trip_leave").equalsIgnoreCase("null")) {
                areas.add(itemJson.getString("trip_leave"));
            }
        }
        params.put("trip_cnt", areas.size());
        // 联系人所在区域个数
        JSONArray contactRegion = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CONTACT_REGION.getValue());
        for (Object item : contactRegion) {
            JSONObject itemJson = (JSONObject) item;
            String key = itemJson.getString("key");
            if (null != key && key.equals("contact_region_6m")) {
                params.put("region_cnt", itemJson.getJSONArray("region_list").size());
            }
        }
        try {
            riskModelOperatorReportDao.saveCallSilentAreas(params);
        } catch (Exception e) {
            log.error("[模型数据-生成]：genCallSilentAreas插入数据出错,nid:{}", request.getNid());
            e.printStackTrace();
        }
    }

    /**
     * 保存树美多头借贷信息
     *
     * @param request
     * @throws Exception
     */
    private void saveSmBorrow(DecisionHandleRequest request) throws Exception {
        JSONObject operatorInfo = mongoHandler.getShumeiMultipoint(request);
        if (null == operatorInfo || !operatorInfo.containsKey("detail")) {
            throw new Exception("树美多头借贷为空");
        }

        JSONObject detail = operatorInfo.getJSONObject("detail");
        StaSmBorrows smBorrows = new StaSmBorrows();

        smBorrows.setApplications(detail.getInteger("itfin_loan_applications"));
        smBorrows.setApplications180d(detail.getInteger("itfin_loan_applications_180d"));
        smBorrows.setApplications30d(detail.getInteger("itfin_loan_applications_30d"));
        smBorrows.setApplications60d(detail.getInteger("itfin_loan_applications_60d"));
        smBorrows.setApplications7d(detail.getInteger("itfin_loan_applications_7d"));
        smBorrows.setApplications90d(detail.getInteger("itfin_loan_applications_90d"));
        smBorrows.setApprovals(detail.getInteger("itfin_loan_approvals"));
        smBorrows.setApprovals180d(detail.getInteger("itfin_loan_approvals_180d"));
        smBorrows.setApprovals30d(detail.getInteger("itfin_loan_approvals_30d"));
        smBorrows.setApprovals60d(detail.getInteger("itfin_loan_approvals_60d"));
        smBorrows.setApprovals7d(detail.getInteger("itfin_loan_approvals_7d"));
        smBorrows.setApprovals90d(detail.getInteger("itfin_loan_approvals_90d"));
        smBorrows.setQueries(detail.getInteger("itfin_loan_queries"));
        smBorrows.setQueries180d(detail.getInteger("itfin_loan_queries_180d"));
        smBorrows.setQueries30d(detail.getInteger("itfin_loan_queries_30d"));
        smBorrows.setQueries60d(detail.getInteger("itfin_loan_queries_60d"));
        smBorrows.setQueries7d(detail.getInteger("itfin_loan_queries_7d"));
        smBorrows.setQueries90d(detail.getInteger("itfin_loan_queries_90d"));
        smBorrows.setRefuses(detail.getInteger("itfin_loan_refuses"));
        smBorrows.setRefuses180d(detail.getInteger("itfin_loan_refuses_180d"));
        smBorrows.setRefuses30d(detail.getInteger("itfin_loan_refuses_30d"));
        smBorrows.setRefuses60d(detail.getInteger("itfin_loan_refuses_60d"));
        smBorrows.setRefuses7d(detail.getInteger("itfin_loan_refuses_7d"));
        smBorrows.setRefuses90d(detail.getInteger("itfin_loan_refuses_90d"));
        smBorrows.setRegisters(detail.getInteger("itfin_registers"));
        smBorrows.setRegisters180d(detail.getInteger("itfin_registers_180d"));
        smBorrows.setRegisters30d(detail.getInteger("itfin_registers_30d"));
        smBorrows.setRegisters60d(detail.getInteger("itfin_registers_60d"));
        smBorrows.setRegisters7d(detail.getInteger("itfin_registers_7d"));
        smBorrows.setRegisters90d(detail.getInteger("itfin_registers_90d"));
        smBorrows.setNid(request.getNid());
        staSmBorrowsDao.saveOrUpdate(smBorrows);
    }
}

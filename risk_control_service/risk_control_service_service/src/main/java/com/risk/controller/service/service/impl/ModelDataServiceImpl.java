package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.DateConvert;
import com.risk.controller.service.common.utils.DateTools;
import com.risk.controller.service.common.utils.IdcardUtils;
import com.risk.controller.service.common.utils.PhoneUtils;
import com.risk.controller.service.dao.StaOperatorCallsDao;
import com.risk.controller.service.dao.StaUserBaseinfoDao;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.entity.StaOperatorCalls;
import com.risk.controller.service.entity.StaUserBaseinfo;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.handler.MongoHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.ModelDataService;
import com.risk.controller.service.service.ThirdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

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

    /**
     * 入口方法
     *
     * @param request
     */
    @Override
    public void saveData(DecisionHandleRequest request) throws Exception {
        this.saveOperatorCalls(request);
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
        if (null == operatorInfo ) {
            throw new Exception("operatorInfo为空");
        }
        if (null == callDetails  || callDetails.size()<=0) {
            throw new Exception("callDetails为空");
        }
        if (null == contactDetails  || contactDetails.size()<=0) {
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
                        eachManNum ++;
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

}

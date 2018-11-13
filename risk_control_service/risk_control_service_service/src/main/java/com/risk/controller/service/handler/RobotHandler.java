package com.risk.controller.service.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.*;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.mongo.dao.MongoCollections;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.ModelDataService;
import com.risk.controller.service.service.ModelService;
import com.risk.controller.service.service.OperatorService;
import com.risk.controller.service.service.WanshuService;
import com.risk.controller.service.service.impl.LocalCache;
import com.risk.controller.service.util.AdmissionHandler;
import com.risk.controller.service.utils.Average;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

/**
 * 模型
 */
@Slf4j
@Service
public class RobotHandler implements AdmissionHandler {
    private final Integer DAY_10 = 10; // 10天
    private final Integer DAY_30 = 30; // 30天
    private final Integer DAY_60 = 60; // 60天
    private final String CALLED = "DIALED"; //被叫
    private final String CALL = "DIAL"; //主叫

    @Autowired
    private RobotRuleDao robotRuleDao;
    @Autowired
    private RobotRuleDetailDao robotRuleDetailDao;
    @Autowired
    private LocalCache localCache;
    @Autowired
    private MongoHandler mongoHandler;
    @Autowired
    private BlacklistPhoneDao blacklistPhoneDao;
    @Autowired
    private AdmissionRuleDao admissionRuleDao;
    @Autowired
    private RobotResultDetailDao robotResultDetailDao;
    @Autowired
    private RobotResultDao robotResultDao;
    @Autowired
    private WanshuService wanshuService;
    @Autowired
    private DecisionReqLogDao decisionReqLogDao;
    @Autowired
    private ModelService modelService;
    @Autowired
    private OperatorService operatorService;
    @Autowired
    private ModelDataService modelDataService;

    /**
     * 1057 模型
     * {"passPercent":"0.34","passCount":"0","randomNum":"-1"}
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyRobot(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        // 随机数内，执行本地模型
        if (rule != null && rule.getSetting() != null && rule.getSetting().containsKey("randomNum")) {
            request.getRobotRequestDTO().setModelNum(2);
            int rulePercent = Integer.valueOf(rule.getSetting().get("randomNum"));
            int randomNum = new Random().nextInt(100) + 1;
            if (rulePercent >= randomNum) {
                return this.verifyRobotV2(request, rule);
            }
        }
        // 其他跳过，执行
        request.getRobotRequestDTO().setModelNum(1);
        AdmissionResultDTO result = new AdmissionResultDTO();
        result.setResult(AdmissionResultDTO.RESULT_SKIP);
        result.setData(request.getRobotRequestDTO().getModelNum());
        return result;

    }


    public AdmissionResultDTO verifyRobotV2(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
        // 验证模型
        if (null == rule || !rule.getSetting().containsKey("passScore")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        BigDecimal rulePassScore = new BigDecimal(rule.getSetting().get("passScore"));
        BigDecimal userScore = BigDecimal.ZERO;
        try {
            List<RobotResultDetail> listRobot = new ArrayList<>();

            // 查询所有模型规则
            List<RobotRule> ruleList = robotRuleDao.getAllrobotRule(null);
            for (RobotRule robotRule : ruleList) {

                if (StringUtils.isBlank(robotRule.getHandler())) {
                    continue;
                }

                int delimiterInd = robotRule.getHandler().lastIndexOf('.');
                String className = robotRule.getHandler().substring(0, delimiterInd); // 处理类
                String methodName = robotRule.getHandler().substring(delimiterInd + 1); // 处理方法

                // 获取对象
                Object handlerObj = this.getHandler(className);
                if (null == handlerObj) {
                    continue;
                }
                // 获取对象方法
                Method methodObj = this.getHandlerMethod(handlerObj, methodName);
                if (null == methodObj) {
                    continue;
                }

                // 执行对象方法（返回对应的值）
                Object count = methodObj.invoke(handlerObj, request);
                if (null == count) {
                    continue;
                }

                // 查询方法返回值对应的规则明细
                RobotRuleDetail detail = robotRuleDetailDao.getDetailByCondition(robotRule.getId(), count);

                if (null == detail) {
                    continue;
                }

                BigDecimal ruleResult = BigDecimal.ZERO;
                if (null != robotRule && robotRule.getPercent().compareTo(BigDecimal.ZERO) > 0 &&
                        null != detail && detail.getOverduePercent().compareTo(BigDecimal.ZERO) > 0) {

                    ruleResult = robotRule.getPercent().multiply(detail.getOverduePercent());
                    userScore = userScore.add(ruleResult);
                }

                RobotResultDetail robotResultDetail = new RobotResultDetail(detail.getId(), count, ruleResult);
                listRobot.add(robotResultDetail);
            }

            result.setData(userScore);
            if (userScore.compareTo(rulePassScore) >= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            }

            RobotResult robotResult = new RobotResult(request.getNid(), userScore, result.getResult(), request.getRobotRequestDTO().getSource());
            robotResultDao.insert(robotResult);
            if (listRobot.size() > 0) {
                listRobot.forEach(robot -> robot.setResultId(robotResult.getId()));
                robotResultDetailDao.saveBatch(listRobot);
            }
            return result;
        } catch (Exception e) {
            log.error("模型异常，request;{},error", request);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("模型异常");
            return result;
        }
    }

    /**
     * 通过反射获取类
     *
     * @param className
     * @return
     */
    private Object getHandler(String className) {
        Object ret = null;
        if (!StringUtils.isEmpty(className)) {
            Class clazz = null;
            try {
                clazz = Class.forName(className);
                ret = ContextUtils.getApplicationContext().getBean(clazz);
            } catch (ClassNotFoundException e) {
                log.error("未查询到类, className:{}", className);
            }
        }
        return ret;
    }

    /**
     * 通过反射获取方法
     *
     * @param handlerObj
     * @param methodName
     * @return
     */
    private Method getHandlerMethod(Object handlerObj, String methodName) {
        Method ret = null;
        try {
            ret = handlerObj.getClass().getDeclaredMethod(methodName, new Class[]{DecisionHandleRequest.class});
        } catch (Exception e) {
            log.error("未查询到类方法, handlerObj:{}, methodName:{}", handlerObj, methodName);
        }
        return ret;
    }

    /**
     * 验证年龄
     *
     * @param request
     * @return
     */
    public Integer robotAge(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            StaUserBaseinfo baseinfo = modelDataService.getUserBaseInfo(request);
            if (null != baseinfo) {
                count = baseinfo.getAge();
            }
        } catch (Exception e) {
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 设备是否多人使用
     *
     * @param request
     * @return
     */
    public Integer robotDeviceUsed(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getDeviceUsedCount()) {
                count = request.getRobotRequestDTO().getDeviceUsedCount();
            } else {
                StaUserBaseinfo baseinfo = modelDataService.getUserBaseInfo(request);
                if (null != baseinfo) {
                    count = baseinfo.getUserDeviceUsedNum();
                }
            }
        } catch (Exception e) {
            log.error("模型：设备是否多人使用异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 检查申请人使用设备的个数
     *
     * @param request
     * @return
     */
    public Integer robotDeviceCount(DecisionHandleRequest request) {
        Integer count = 0;
        try {

            if (null != request.getRobotRequestDTO().getUserDeviceCount()) {
                count = request.getRobotRequestDTO().getUserDeviceCount();
            } else {
                StaUserBaseinfo baseinfo = modelDataService.getUserBaseInfo(request);
                if (null != baseinfo) {
                    count = baseinfo.getUserDeviceNum();
                }
            }
        } catch (Exception e) {
            log.error("模型：检查申请人使用设备的个数异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 通讯录中注册人数
     * @param request
     * @return
     */
    public Integer robotCntRegisterCount(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            StaUserBaseinfo baseinfo = modelDataService.getUserBaseInfo(request);
            if (null != baseinfo) {
                count = baseinfo.getCntRegisterNum();
            }
        } catch (Exception e) {
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 通讯录中联系人数量-手机
     *
     * @param request
     * @return
     */
    public Integer robotCntCount(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserDeviceContacCount()) {
                count = request.getRobotRequestDTO().getUserDeviceContacCount();
            } else {
                StaUserBaseinfo baseinfo = modelDataService.getUserBaseInfo(request);
                if (null != baseinfo) {
                    count = baseinfo.getCntNum();
                }
            }
        } catch (Exception e) {
            log.error("模型：通讯录中联系人数量异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 运营商短号验证
     *
     * @param request
     * @return
     */
    public Integer robotOptShortNum(DecisionHandleRequest request) {
        int count = 0;
        try {

            if (null != request.getRobotRequestDTO().getUserShortNumCount()) {
                count = request.getRobotRequestDTO().getUserShortNumCount();
            } else {
                StaUserBaseinfo baseinfo = modelDataService.getUserBaseInfo(request);
                if (null != baseinfo) {
                    count = baseinfo.getOptShortNum();
                }
            }
        } catch (Exception e) {
            log.error("模型：运营商通话记录验证黑名单异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 手机号码使用时间
     *
     * @param request
     * @return
     */
    public Integer robotOptPhoneUsedTime(DecisionHandleRequest request) {
        int count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserOpertorPhoneUsedTime()) {
                count = request.getRobotRequestDTO().getUserOpertorPhoneUsedTime();
            } else {
                StaUserBaseinfo baseinfo = modelDataService.getUserBaseInfo(request);
                if (null != baseinfo) {
                    count = baseinfo.getDuration();
                }
            }
        } catch (Exception e) {
            log.error("模型：手机号码使用时间异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 运营商平均话费验证（分）
     *
     * @param request
     * @return
     */
    public Integer robotOptAvgFee(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserOperatorAvgCharge()) {
                count = request.getRobotRequestDTO().getUserOperatorAvgCharge();
            } else {
                StaUserBaseinfo baseinfo = modelDataService.getUserBaseInfo(request);
                if (null != baseinfo) {
                    count = baseinfo.getOptAvgFee();
                }
            }
        } catch (Exception e) {
            log.error("模型：运营商平均话费验证（分）异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 运营商通话记录注册人个数
     * @param request
     * @return
     */
    public Integer robotOptRegisterNum(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            StaUserBaseinfo baseinfo = modelDataService.getUserBaseInfo(request);
            if (null != baseinfo) {
                count = baseinfo.getOptCallsRegisterNum();
            }
        } catch (Exception e) {
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 树美提出过申请网贷平台个数-7天
     *
     * @param request
     * @return
     */
    public Integer robotShumeiMultiNum7(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserShumeiCount()) {
                count = request.getRobotRequestDTO().getUserShumeiCount();
            } else {
                StaSmBorrows staSmBorrows = modelDataService.getStaSmBorrows(request);
                if (null != staSmBorrows) {
                    count = staSmBorrows.getApplications7d();
                }
            }
        } catch (Exception e) {
            log.error("模型：树美多头借贷异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 树美提出过申请网贷平台个数-30天
     *
     * @param request
     * @return
     */
    public Integer robotShumeiMultiNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserShumeiCount()) {
                count = request.getRobotRequestDTO().getUserShumeiCount();
            } else {
                StaSmBorrows staSmBorrows = modelDataService.getStaSmBorrows(request);
                if (null != staSmBorrows) {
                    count = staSmBorrows.getApplications30d();
                }
            }
        } catch (Exception e) {
            log.error("模型：树美多头借贷异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 树美提出过申请网贷平台个数-30天
     *
     * @param request
     * @return
     */
    public Integer robotShumeiMultiNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserShumeiCount()) {
                count = request.getRobotRequestDTO().getUserShumeiCount();
            } else {
                StaSmBorrows staSmBorrows = modelDataService.getStaSmBorrows(request);
                if (null != staSmBorrows) {
                    count = staSmBorrows.getApplications60d();
                }
            }
        } catch (Exception e) {
            log.error("模型：树美多头借贷异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 树美提出过申请网贷平台个数-30天
     *
     * @param request
     * @return
     */
    public Integer robotShumeiMultiNum(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserShumeiCount()) {
                count = request.getRobotRequestDTO().getUserShumeiCount();
            } else {
                StaSmBorrows staSmBorrows = modelDataService.getStaSmBorrows(request);
                if (null != staSmBorrows) {
                    count = staSmBorrows.getApplications();
                }
            }
        } catch (Exception e) {
            log.error("模型：树美多头借贷异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内主叫次数-手机
     */
    public Integer robotCntCallNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntCallNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天内主叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内主叫时长-手机
     */
    public Integer robotCntCallTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntCallTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天内主叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内主叫人次-手机运营商
     *
     * @param request
     * @return
     */
    public Integer robotCntCallManNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntCallManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内被叫次数-手机
     */
    public Integer robotCntCalledNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntCalledNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天内被叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内被叫时长-手机
     */
    public Integer robotCntCalledTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntCalledTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天内被叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内被叫人次-手机
     *
     * @param request
     * @return
     */
    public Integer robotCntCalledManNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntCalledManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内互通次数-手机
     *
     * @return
     */
    public Integer robotCntEachOtherNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntEachOtherNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天内互通次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内互通时长-手机
     */
    public Integer robotCntEachOtherTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntEachOtherTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天内互通时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内互通人次-手机
     */
    public Integer robotCntEachOtherManNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntEachOtherManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天内互通人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天内通话时长和次数比值-手机
     */
    public Object robotCntEachOtherPercent10(DecisionHandleRequest request) {
        Object count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        Integer num = calls.getCntEachOtherNum();
                        Integer time = calls.getCntEachOtherTime();
                        if (num != 0 && time != 0) {
                            count = new BigDecimal(time).divide(new BigDecimal(num), 4, BigDecimal.ROUND_HALF_UP);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天内通话时长和次数比值-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内主叫次数-手机
     */
    public Integer robotCntCallNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntCallNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天内主叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内主叫时长-手机
     */
    public Integer robotCntCallTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntCallTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天内主叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内主叫人次-手机运营商
     *
     * @param request
     * @return
     */
    public Integer robotCntCallManNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntCallManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内被叫次数-手机
     */
    public Integer robotCntCalledNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntCalledNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天内被叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内被叫时长-手机
     */
    public Integer robotCntCalledTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntCalledTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天内被叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内被叫人次-手机
     *
     * @param request
     * @return
     */
    public Integer robotCntCalledManNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntCalledManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内互通次数-手机
     *
     * @return
     */
    public Integer robotCntEachOtherNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntEachOtherNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天内互通次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内互通时长-手机
     */
    public Integer robotCntEachOtherTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntEachOtherTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天内互通时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内互通人次-手机
     */
    public Integer robotCntEachOtherManNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntEachOtherManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天内互通人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天内通话时长和次数比值-手机
     */
    public Object robotCntEachOtherPercent30(DecisionHandleRequest request) {
        Object count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        Integer num = calls.getCntEachOtherNum();
                        Integer time = calls.getCntEachOtherTime();
                        if (num != 0 && time != 0) {
                            count = new BigDecimal(time).divide(new BigDecimal(num), 4, BigDecimal.ROUND_HALF_UP);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天内通话时长和次数比值-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内主叫次数-手机
     */
    public Integer robotCntCallNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntCallNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天内主叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内主叫时长-手机
     */
    public Integer robotCntCallTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntCallTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天内主叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内主叫人次-手机运营商
     *
     * @param request
     * @return
     */
    public Integer robotCntCallManNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntCallManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内被叫次数-手机
     */
    public Integer robotCntCalledNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntCalledNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天内被叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内被叫时长-手机
     */
    public Integer robotCntCalledTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntCalledTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天内被叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内被叫人次-手机
     *
     * @param request
     * @return
     */
    public Integer robotCntCalledManNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntCalledManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内互通次数-手机
     *
     * @return
     */
    public Integer robotCntEachOtherNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntEachOtherNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天内互通次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内互通时长-手机
     */
    public Integer robotCntEachOtherTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntEachOtherTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天内互通时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内互通人次-手机
     */
    public Integer robotCntEachOtherManNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntEachOtherManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天内互通人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天内通话时长和次数比值-手机
     */
    public Object robotCntEachOtherPercent60(DecisionHandleRequest request) {
        Object count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        Integer num = calls.getCntEachOtherNum();
                        Integer time = calls.getCntEachOtherTime();
                        if (num != 0 && time != 0) {
                            count = new BigDecimal(time).divide(new BigDecimal(num), 4, BigDecimal.ROUND_HALF_UP);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天内通话时长和次数比值-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商主叫次数-手机
     * @param request
     * @return
     */
    public Integer robotOptCallNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptCallNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商主叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商主叫时长-手机
     * @param request
     * @return
     */
    public Integer robotOptCallTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptCallTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商主叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商主叫人次-手机
     * @param request
     * @return
     */
    public Integer robotOptCallManNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptCallManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商主叫人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商被叫次数-手机
     * @param request
     * @return
     */
    public Integer robotOptCalledNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptCalledNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商被叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商被叫时长-手机
     * @param request
     * @return
     */
    public Integer robotOptCalledTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptCalledTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商被叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商被叫人次-手机
     * @param request
     * @return
     */
    public Integer robotOptCalledManNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptCalledManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商被叫人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商总通话次数-手机
     * @param request
     * @return
     */
    public Integer robotOptNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptCalledNum() + calls.getOptCallNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商总通话次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商总通话时长-手机
     * @param request
     * @return
     */
    public Integer robotOptTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptCalledTime() + calls.getOptCallTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商总通话时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商总通话人次-手机
     * @param request
     * @return
     */
    public Integer robotOptManNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商总通话人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商短信发送个数-手机
     * @param request
     * @return
     */
    public Integer robotOptSmsSendNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptSmsSendNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商短信发送个数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商短信接收个数-手机
     * @param request
     * @return
     */
    public Integer robotOptSmsReceiveNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptSmsReceiveNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商短信接收个数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天运营商短信通讯人次
     * @param request
     * @return
     */
    public Integer robotOptSmsManNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getOptSmsManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天运营商短信通讯人次异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天通讯录有效通话次数-手机（主叫+被叫）
     * @param request
     * @return
     */
    public Integer robotCntValidNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntValidNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天通讯录有效通话次数-手机（主叫+被叫）异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 110天通讯录有效通话时长-手机（主叫+被叫）
     * @param request
     * @return
     */
    public Integer robotCntValidTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntValidTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天通讯录有效通话时长-手机（主叫+被叫）异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天通讯录有效通话人次-手机
     * @param request
     * @return
     */
    public Integer robotCntValidManNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        count = calls.getCntValidManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天通讯录有效通话人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 10天通讯录有效通话时长/次数比值-手机
     * @param request
     * @return
     */
    public Object robotCntValidPercent10(DecisionHandleRequest request) {
        Object count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_10.equals(calls.getDay())) {
                        Integer num = calls.getCntValidNum();
                        Integer time = calls.getCntValidTime();
                        if (num != 0 && time != 0) {
                            count = new BigDecimal(time).divide(new BigDecimal(num), 4, BigDecimal.ROUND_HALF_UP);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：10天通讯录有效通话时长/次数比值-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }


    /**
     * 30天运营商主叫次数-手机
     * @param request
     * @return
     */
    public Integer robotOptCallNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptCallNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商主叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商主叫时长-手机
     * @param request
     * @return
     */
    public Integer robotOptCallTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptCallTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商主叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商主叫人次-手机
     * @param request
     * @return
     */
    public Integer robotOptCallManNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptCallManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商主叫人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商被叫次数-手机
     * @param request
     * @return
     */
    public Integer robotOptCalledNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptCalledNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商被叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商被叫时长-手机
     * @param request
     * @return
     */
    public Integer robotOptCalledTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptCalledTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商被叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商被叫人次-手机
     * @param request
     * @return
     */
    public Integer robotOptCalledManNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptCalledManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商被叫人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商总通话次数-手机
     * @param request
     * @return
     */
    public Integer robotOptNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptCalledNum() + calls.getOptCallNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商总通话次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商总通话时长-手机
     * @param request
     * @return
     */
    public Integer robotOptTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptCalledTime() + calls.getOptCallTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商总通话时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商总通话人次-手机
     * @param request
     * @return
     */
    public Integer robotOptManNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商总通话人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商短信发送个数-手机
     * @param request
     * @return
     */
    public Integer robotOptSmsSendNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptSmsSendNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商短信发送个数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商短信接收个数-手机
     * @param request
     * @return
     */
    public Integer robotOptSmsReceiveNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptSmsReceiveNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商短信接收个数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天运营商短信通讯人次
     * @param request
     * @return
     */
    public Integer robotOptSmsManNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getOptSmsManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天运营商短信通讯人次异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天通讯录有效通话次数-手机（主叫+被叫）
     * @param request
     * @return
     */
    public Integer robotCntValidNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntValidNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天通讯录有效通话次数-手机（主叫+被叫）异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 130天通讯录有效通话时长-手机（主叫+被叫）
     * @param request
     * @return
     */
    public Integer robotCntValidTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntValidTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天通讯录有效通话时长-手机（主叫+被叫）异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天通讯录有效通话人次-手机
     * @param request
     * @return
     */
    public Integer robotCntValidManNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        count = calls.getCntValidManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天通讯录有效通话人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 30天通讯录有效通话时长/次数比值-手机
     * @param request
     * @return
     */
    public Object robotCntValidPercent30(DecisionHandleRequest request) {
        Object count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_30.equals(calls.getDay())) {
                        Integer num = calls.getCntValidNum();
                        Integer time = calls.getCntValidTime();
                        if (num != 0 && time != 0) {
                            count = new BigDecimal(time).divide(new BigDecimal(num), 4, BigDecimal.ROUND_HALF_UP);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：30天通讯录有效通话时长/次数比值-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }


    /**
     * 60天运营商主叫次数-手机
     * @param request
     * @return
     */
    public Integer robotOptCallNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptCallNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商主叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商主叫时长-手机
     * @param request
     * @return
     */
    public Integer robotOptCallTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptCallTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商主叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商主叫人次-手机
     * @param request
     * @return
     */
    public Integer robotOptCallManNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptCallManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商主叫人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商被叫次数-手机
     * @param request
     * @return
     */
    public Integer robotOptCalledNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptCalledNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商被叫次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商被叫时长-手机
     * @param request
     * @return
     */
    public Integer robotOptCalledTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptCalledTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商被叫时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商被叫人次-手机
     * @param request
     * @return
     */
    public Integer robotOptCalledManNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptCalledManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商被叫人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商总通话次数-手机
     * @param request
     * @return
     */
    public Integer robotOptNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptCalledNum() + calls.getOptCallNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商总通话次数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商总通话时长-手机
     * @param request
     * @return
     */
    public Integer robotOptTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptCalledTime() + calls.getOptCallTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商总通话时长-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商总通话人次-手机
     * @param request
     * @return
     */
    public Integer robotOptManNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商总通话人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商短信发送个数-手机
     * @param request
     * @return
     */
    public Integer robotOptSmsSendNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptSmsSendNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商短信发送个数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商短信接收个数-手机
     * @param request
     * @return
     */
    public Integer robotOptSmsReceiveNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptSmsReceiveNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商短信接收个数-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天运营商短信通讯人次
     * @param request
     * @return
     */
    public Integer robotOptSmsManNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getOptSmsManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天运营商短信通讯人次异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天通讯录有效通话次数-手机（主叫+被叫）
     * @param request
     * @return
     */
    public Integer robotCntValidNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntValidNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天通讯录有效通话次数-手机（主叫+被叫）异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 160天通讯录有效通话时长-手机（主叫+被叫）
     * @param request
     * @return
     */
    public Integer robotCntValidTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntValidTime();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天通讯录有效通话时长-手机（主叫+被叫）异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天通讯录有效通话人次-手机
     * @param request
     * @return
     */
    public Integer robotCntValidManNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        count = calls.getCntValidManNum();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天通讯录有效通话人次-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }

    /**
     * 60天通讯录有效通话时长/次数比值-手机
     * @param request
     * @return
     */
    public Object robotCntValidPercent60(DecisionHandleRequest request) {
        Object count = 0;
        try {
            List<StaOperatorCalls> list = modelDataService.getOperatorCalls(request);
            if (null != list && list.size() > 0) {
                for (StaOperatorCalls calls : list) {
                    if (DAY_60.equals(calls.getDay())) {
                        Integer num = calls.getCntValidNum();
                        Integer time = calls.getCntValidTime();
                        if (num != 0 && time != 0) {
                            count = new BigDecimal(time).divide(new BigDecimal(num), 4, BigDecimal.ROUND_HALF_UP);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：60天通讯录有效通话时长/次数比值-手机异常，nid;{},error", request.getNid());
        }
        return count;
    }


    /*****************************华丽分割线*****************************/
    /**
     *  通话风险分析-与催收类号码联系情况次数（3个月总次数）
     */
    public Integer robotCallRiskAnalysisCollection(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray call_risk_analysis = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_RISK_ANALYSIS.getValue());
            if (null == call_risk_analysis || call_risk_analysis.size() == 0) {
                return null;
            }

            for (Object item : call_risk_analysis) {
                JSONObject itemJson = (JSONObject) item;
                if (null == itemJson) {
                    continue;
                }
                // 催收公司
                if ("collection".equals(itemJson.getString(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_ITEM.getValue()))) {
                    JSONObject analysis_point =  itemJson.getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_POINT.getValue());
                    count = analysis_point.getInteger(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_CNT_3M.getValue());
                }
            }
        } catch (Exception e) {
            log.error("模型：通话风险分析-与催收类号码联系情况次数，nid;{},error", request.getNid());
        }

        return count;
    }

    /**
     *  通话风险分析-与信用卡号码联系情况次数（3个月总次数）
     */
    public Integer robotCallRiskAnalysisCreditCard(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray call_risk_analysis = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_RISK_ANALYSIS.getValue());
            if (null == call_risk_analysis || call_risk_analysis.size() == 0) {
                return null;
            }

            for (Object item : call_risk_analysis) {
                JSONObject itemJson = (JSONObject) item;

                if (null == itemJson) {
                    continue;
                }
                // 信用卡
                if ("credit_card".equals(itemJson.getString(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_ITEM.getValue()))) {
                    JSONObject analysis_point =  itemJson.getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_POINT.getValue());
                    count = analysis_point.getInteger(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_CNT_3M.getValue());
                }
            }
        } catch (Exception e) {
            log.error("模型：通话风险分析-与信用卡类号码联系情况次数，nid;{},error", request.getNid());
        }

        return count;
    }

    /**
     *  通话风险分析-与贷款类号码联系情况次数（3个月总次数）
     */
    public Integer robotCallRiskAnalysisLoan(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray call_risk_analysis = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_RISK_ANALYSIS.getValue());
            if (null == call_risk_analysis || call_risk_analysis.size() == 0) {
                return null;
            }

            for (Object item : call_risk_analysis) {
                JSONObject itemJson = (JSONObject) item;
                if (null == itemJson) {
                    continue;
                }
                // loan
                if ("loan".equals(itemJson.getString(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_ITEM.getValue()))) {
                    JSONObject analysis_point =  itemJson.getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_POINT.getValue());
                    count = analysis_point.getInteger(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_CNT_3M.getValue());
                }
            }
        } catch (Exception e) {
            log.error("模型：通话风险分析-与贷款类号码联系情况次数，nid;{},error", request.getNid());
        }

        return count;
    }

    /**
     *  通话风险分析-110，120，律师、法院等通话次数（3个月总次数）
     */
    public Integer robotCallRiskAnalysisGov(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray call_risk_analysis = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_RISK_ANALYSIS.getValue());
            if (null == call_risk_analysis || call_risk_analysis.size() == 0) {
                return null;
            }

            for (Object item : call_risk_analysis) {
                JSONObject itemJson = (JSONObject) item;
                if (null == itemJson) {
                    continue;
                }
                // 110
                if ("110".equals(itemJson.getString(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_ITEM.getValue()))) {
                    JSONObject analysis_point =  itemJson.getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_POINT.getValue());
                    Integer call_cnt_3m = analysis_point.getInteger(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_CNT_3M.getValue());
                    call_cnt_3m = call_cnt_3m == null ? 0 : call_cnt_3m;
                    count = count + call_cnt_3m;
                }

                // 120
                if ("120".equals(itemJson.getString(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_ITEM.getValue()))) {
                    JSONObject analysis_point =  itemJson.getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_POINT.getValue());
                    Integer call_cnt_3m = analysis_point.getInteger(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_CNT_3M.getValue());
                    call_cnt_3m = call_cnt_3m == null ? 0 : call_cnt_3m;
                    count = count + call_cnt_3m;
                }

                // lawyer
                if ("lawyer".equals(itemJson.getString(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_ITEM.getValue()))) {
                    JSONObject analysis_point =  itemJson.getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_POINT.getValue());
                    Integer call_cnt_3m = analysis_point.getInteger(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_CNT_3M.getValue());
                    call_cnt_3m = call_cnt_3m == null ? 0 : call_cnt_3m;
                    count = count + call_cnt_3m;
                }

                // court
                if ("court".equals(itemJson.getString(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_ITEM.getValue()))) {
                    JSONObject analysis_point =  itemJson.getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.ANALYSIS_POINT.getValue());
                    Integer call_cnt_3m = analysis_point.getInteger(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_CNT_3M.getValue());
                    call_cnt_3m = call_cnt_3m == null ? 0 : call_cnt_3m;
                    count = count + call_cnt_3m;
                }
            }
        } catch (Exception e) {
            log.error("模型：通话风险分析-与110，120，律师、法院等次数，nid;{},error", request.getNid());
        }

        return count;
    }

    /**
     *  通话风险分析-用户号码联系黑中介分数（分数范围0-100，参考分为10，分数越低关系越紧密
     */
    public Integer robotCallCheckBlackInfoScore(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return count;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_BLACK_INFO.getValue());
            if (null == itemJson) {
                return count;
            }
            count = itemJson.getInteger("phone_gray_score");
        } catch (Exception e) {
            log.error("模型：通话风险分析-联系黑中介分数，nid;{},error", request.getNid());
        }

        return count;
    }

    /**
     *  通话风险分析-引起间接黑名单人数
     */
    public Integer robotCallCheckBlackInfoRouter(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return count;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_BLACK_INFO.getValue());
            if (null == itemJson) {
                return count;
            }
            count = itemJson.getInteger("contacts_router_cnt");
        } catch (Exception e) {
            log.error("模型：通话风险分析-引起间接黑名单人数，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     *  通话风险分析-间接联系人中黑名单人数
     */
    public Integer robotCallCheckBlackInfoClass2Cnt(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return count;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_BLACK_INFO.getValue());
            if (null == itemJson) {
                return count;
            }
            count = itemJson.getInteger("contacts_class2_blacklist_cnt");
        } catch (Exception e) {
            log.error("模型：通话风险分析-间接联系人中黑名单人数，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     *  通话风险分析-查询过该用户的相关企业数量（姓名+身份证+电话号码）
     */
    public Integer robotCallSearchedOrgCnt(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return count;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return count;
            }
            count = itemJson.getInteger("searched_org_cnt");
        } catch (Exception e) {
            log.error("模型：通话风险分析-查询过该用户的相关企业数量，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     *  通话风险分析-身份证组合过的其他姓名(返回了匹配数量)
     */
    public Integer robotCallIdcardWithOtherNames(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return count;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return count;
            }
            JSONArray array = itemJson.getJSONArray("idcard_with_other_names");
            if (null == array) {
                return count;
            }
            count = array.size();
        } catch (Exception e) {
            log.error("模型：通话风险分析-身份证组合过的其他姓名，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     *  通话风险分析-身份证组合过其他电话(返回了匹配数量)
     */
    public Integer robotCallIdcardWithOtherPhones(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return count;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return count;
            }
            JSONArray array = itemJson.getJSONArray("idcard_with_other_phones");
            if (null == array) {
                return count;
            }
            count = array.size();
        } catch (Exception e) {
            log.error("模型：通话风险分析-身份证组合过其他电话，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     *  通话风险分析-电话号码组合过其他姓名(返回了匹配数量)
     */
    public Integer robotCallPhoneWithOtherNames(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return count;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return count;
            }
            JSONArray array = itemJson.getJSONArray("phone_with_other_names");
            if (null == array) {
                return count;
            }
            count = array.size();
        } catch (Exception e) {
            log.error("模型：通话风险分析-身份证组合过其他电话，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     *  通话风险分析-电话号码组合过其他身份证(返回了匹配数量)
     */
    public Integer robotCallPhoneWithOtherIdcards(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return count;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return count;
            }
            JSONArray array = itemJson.getJSONArray("phone_with_other_idcards");
            if (null == array) {
                return count;
            }
            count = array.size();
        } catch (Exception e) {
            log.error("模型：通话风险分析-电话号码组合过其他身份证，nid;{},error", request.getNid());
        }

        return count;
    }

    /**
     *  104 出行分析-联系人所在区域个数汇总
     */
    public Integer robotCallContactRegion(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray contactRegion = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CONTACT_REGION.getValue());
            if (null == contactRegion || contactRegion.size() == 0) {
                log.debug("[104维度-空值返回] : null == contactRegion || contactRegion.size() == 0");
                return count;
            }

            for (Object item : contactRegion) {
                JSONObject itemJson = (JSONObject) item;
                String key = itemJson.getString("key");
                if (null != key && key.equals("contact_region_6m")) {
                    log.debug("[104维度-contact_region_6m]:获取到节点值");
                    if (null != itemJson.getJSONArray("region_list")) {
                        count = itemJson.getJSONArray("region_list").size();
                        log.debug("[104维度-计算count]:count = {}", count);
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：出行分析-联系人所在区域个数汇总，nid;{},error", request.getNid());
            e.printStackTrace();
        }
        log.debug("[104维度-count] : count = {}", count);
        return count;
    }

    /**
     *  105 出行分析（外出不同城市的个数，曾在那些城市打过电话）
     */
    public Integer robotCallTripInfo(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.TRIP_INFO.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return count;
            }

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

            count = areas.size();
        } catch (Exception e) {
            log.error("模型：出行分析-外出不同城市的个数，曾在那些城市打过电话，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     *  106 深夜[1:30-5:30]通话总时间,近三个月
     */
    public Integer robotCallDurationDetail(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return count;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;
                if (itemJson.getString("key").equalsIgnoreCase("call_duration_detail_3m")) {
                    JSONArray duration_list = itemJson.getJSONArray("duration_list");
                    for (Object duration : duration_list) {
                        JSONObject durationJson = (JSONObject) duration;
                        if (durationJson.getString("time_step").equalsIgnoreCase("midnight")) {
                            durationJson = durationJson.getJSONObject("item");
                            count = durationJson.getInteger("total_time");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：通话分析-深夜[1:30-5:30]通话总次数，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     *  107 深夜[1:30-5:30]通话数，近三个月
     */
    public Integer robotCallMidnightTotalCnt(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return count;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;
                if (itemJson.getString("key").equalsIgnoreCase("call_duration_detail_3m")) {
                    JSONArray duration_list = itemJson.getJSONArray("duration_list");
                    for (Object duration : duration_list) {
                        JSONObject durationJson = (JSONObject) duration;
                        log.debug("[107维度-duration_list]：duration_list获取到值");
                        if (durationJson.getString("time_step").equalsIgnoreCase("midnight")) {
                            durationJson = durationJson.getJSONObject("item");
                            count = durationJson.getInteger("total_cnt");
                            log.debug("[107维度-count计算]：count = {}", count);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：深夜通话分析-深夜[1:30-5:30]通话总次数，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        log.debug("[107维度-返回值]：count = {}", count);
        return count;
    }

    /**
     *  108 深夜[1:30-5:30]通话号码数，近三个月
     */
    public Integer robotCallMidnightUniqNumCnt(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return count;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;
                if (itemJson.getString("key").equalsIgnoreCase("call_duration_detail_3m")) {
                    JSONArray duration_list = itemJson.getJSONArray("duration_list");
                    log.debug("[108维度-duration_list]：duration_list获取到值");
                    for (Object duration : duration_list) {
                        JSONObject durationJson = (JSONObject) duration;
                        log.debug("[108维度-duration_list]：duration_list获取到值");
                        if (durationJson.getString("time_step").equalsIgnoreCase("midnight")) {
                            durationJson = durationJson.getJSONObject("item");
                            count = durationJson.getInteger("uniq_num_cnt");
                            log.debug("[108维度-count计算]：count = {}", count);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：深夜通话分析-深夜[1:30-5:30]通话号码数，nid;{},error", request.getNid());
            e.printStackTrace();
        }
        log.debug("[108维度-返回值]：count = {}", count);
        return count;
    }

    /**
     *  109 深夜[1:30-5:30]主叫数，近三个月
     */
    public Integer robotCallMidnightDialCnt(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return count;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;
                if (itemJson.getString("key").equalsIgnoreCase("call_duration_detail_3m")) {
                    JSONArray duration_list = itemJson.getJSONArray("duration_list");
                    for (Object duration : duration_list) {
                        JSONObject durationJson = (JSONObject) duration;
                        if (durationJson.getString("time_step").equalsIgnoreCase("midnight")) {
                            durationJson = durationJson.getJSONObject("item");
                            count = durationJson.getInteger("dial_cnt");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：深夜通话分析-深夜[1:30-5:30]主叫数，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     *  110 深夜[1:30-5:30]被叫数，近三个月
     */
    public Integer robotCallMidnightDialedCnt(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                log.debug("[110维度-空值返回] : null == tripInfo || tripInfo.size() == 0");
                return count;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;
                if (itemJson.getString("key").equalsIgnoreCase("call_duration_detail_3m")) {
                    JSONArray duration_list = itemJson.getJSONArray("duration_list");
                    for (Object duration : duration_list) {
                        JSONObject durationJson = (JSONObject) duration;
                        if (durationJson.getString("time_step").equalsIgnoreCase("midnight")) {
                            durationJson = durationJson.getJSONObject("item");
                            count = durationJson.getInteger("dialed_cnt");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：深夜通话分析-深夜[1:30-5:30]被叫数，nid;{},error", request.getNid());
            e.printStackTrace();
        }
        log.debug("[110维度-返回值] : count = {}", count);
        return count;
    }

    /**
     *  111 行为分析-手机静默情况
     */
    public Integer robotCallPhoneSilent(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = modelDataService.getOperatorReport(request);
            JSONArray behaviorCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.BEHAVIOR_CHECK.getValue());
            if (null == behaviorCheck || behaviorCheck.size() == 0) {
                return count;
            }

            for (Object item : behaviorCheck) {
                JSONObject itemJson = (JSONObject) item;

                if (StringUtils.isNotEmpty(itemJson.getString("check_point"))
                        && !itemJson.getString("check_point").equalsIgnoreCase("phone_silent")) {
                    count = itemJson.getInteger("score");
                }
            }
        } catch (Exception e) {
            log.error("模型：行为分析-手机静默情况，nid;{},error", request.getNid());
            e.printStackTrace();
        }

        return count;
    }

    /**
     * 通过sql注入批量跑模型数据(必须包含nid)
     * select nid from table
     *
     * @param sql
     */
    @Async
    public void runModelBySql(String sql) {
        List<Map<String, Object>> list = this.modelService.runModelBySql(sql);

        if (null != list && list.size() > 0) {

            AdmissionRule rule = admissionRuleDao.getByRuleId(1057L);
            AdmissionRuleDTO ruleDto = AdmissionRuleDTO.fromAdmissionRule(rule);
            ruleDto.getSetting().put("randomNum", "100");

            if (null != ruleDto) {
                for (Map<String, Object> map : list) {
                    Object nidObject = map.get("nid");
                    if (null != nidObject) {
                        String nid = (String) nidObject;
                        DecisionReqLog reqLog = decisionReqLogDao.getbyNid(nid);
                        if (null != reqLog) {
                            DecisionHandleRequest request = JSONObject.parseObject(reqLog.getReqData(), DecisionHandleRequest.class);
                            request.getRobotRequestDTO().setSource(RobotResult.SOURCE_2);
                            AdmissionResultDTO record = this.verifyRobot(request, ruleDto);
                            log.debug("模型重跑结果：nid：{}，结果：{}", nid, JSONObject.toJSONString(record));
                        }
                    }
                }
            }
        }
    }
}

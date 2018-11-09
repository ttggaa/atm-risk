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
        if (null == rule
                || !rule.getSetting().containsKey("passPercent")
                || !rule.getSetting().containsKey("passCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        BigDecimal rulePercent = new BigDecimal(rule.getSetting().get("passPercent"));
        Integer rulePassCount = Integer.valueOf(rule.getSetting().get("passCount"));
        Integer userPassCount = 0;
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

                // 查询方法返回值对应的规则明细
                RobotRuleDetail detail = robotRuleDetailDao.getDetailByCondition(robotRule.getId(), count);

                if (null == detail) {
                    continue;
                }
                int ruleResult = 0;
                if (null != detail
                        && detail.getOverduePercent().compareTo(rulePercent) <= 0
                        && detail.getOverduePercent().compareTo(BigDecimal.ZERO) > 0) {

                    userPassCount++;
                    ruleResult = 1;
                }

                RobotResultDetail robotResultDetail = new RobotResultDetail(detail.getId(), count, ruleResult);
                listRobot.add(robotResultDetail);
            }


            result.setData(userPassCount);
            if (userPassCount >= rulePassCount) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            } else {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            }

            RobotResult robotResult = new RobotResult(request.getNid(), userPassCount, result.getResult(), request.getRobotRequestDTO().getSource());
            robotResultDao.insert(robotResult);
            if (listRobot.size() > 0) {
                listRobot.forEach(robot -> robot.setResultId(robotResult.getId()));
                robotResultDetailDao.saveBatch(listRobot);
            }
            return result;
        } catch (Exception e) {
            log.error("模型异常，request;{},error", request, e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("模型异常");
            return result;
        }
    }


    private Integer robotCallAndCalledNum7(DecisionHandleRequest request, Integer days) {
        Integer count = 0;
        try {
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), days);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：" + days + "天内互通时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
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
            log.error("模型：用户手机连号验证异常，nid;{},error", request.getNid(), e);
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
            log.error("模型：设备是否多人使用异常，nid;{},error", request.getNid(), e);
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
                JSONObject rs = this.getDeviceCount(request.getUserId());
                if (null != rs && null != rs.get("data") && "0".equals(rs.getString("code"))) {
                    count = rs.getInteger("data");
                }
            }
        } catch (Exception e) {
            log.error("模型：检查申请人使用设备的个数异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 查询用户的设备被多少个用户使用
     *
     * @param userId
     * @return
     */
    private JSONObject getDeviceUsageCount(Long userId) {
        if (null != userId && 0L != userId) {
            String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.deviceUsage.url");
            Map<String, String> params = new HashMap<>();
            params.put("userId", String.valueOf(userId));
            try {
                String resultStr = HttpClientUtils.doPost(url, JSONObject.toJSONString(params), "application/json");
                JSONObject json = JSONObject.parseObject(resultStr);
                return json;
            } catch (Throwable e) {
                log.error("查询用户的设备被多少个用户使用异常，userId:{},e:{}", userId, e);
            }
        }
        return null;
    }

    /**
     * 查询用户设备个数
     *
     * @param userId
     * @return
     */
    private JSONObject getDeviceCount(Long userId) {
        if (null != userId && 0L != userId) {
            String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.userDeviceUsage.url");
            Map<String, String> params = new HashMap<>();
            params.put("userId", String.valueOf(userId));
            try {
                String resultStr = HttpClientUtils.doPost(url, JSONObject.toJSONString(params), "application/json");
                JSONObject json = JSONObject.parseObject(resultStr);
                return json;
            } catch (Throwable e) {
                log.error("查询设备个数异常，userId:{},e:{}", userId, e);
            }
        }
        return null;
    }

    /**
     * 通讯录中联系人数量
     *
     * @param request
     * @return
     */
    public Integer robotDeviceContactCount(DecisionHandleRequest request) {
        Integer count = 0;
        try {

            if (null != request.getRobotRequestDTO().getUserDeviceContacCount()) {
                count = request.getRobotRequestDTO().getUserDeviceContacCount();
            } else {
                List<JSONObject> deviceContact = this.mongoHandler.getUserDeviceContact(request);
                // 手机号码去重，并验证是否11位
                Set<String> set = new HashSet<>();
                if (null != deviceContact || deviceContact.size() > 0) {
                    for (JSONObject contact : deviceContact) {
                        String phone = contact.getString("contactsPhone");
                        phone = PhoneUtils.cleanTel(phone);
                        if (PhoneUtils.isMobile(phone)) {
                            set.add(phone);
                        }
                    }
                }
                count = set.size();
            }
        } catch (Exception e) {
            log.error("模型：通讯录中联系人数量异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 通讯录、运营商、通话记录短号验证
     *
     * @param request
     * @return
     */
    public Integer robotShortNumCount(DecisionHandleRequest request) {
        int count = 0;
        try {

            if (null != request.getRobotRequestDTO().getUserShortNumCount()) {
                count = request.getRobotRequestDTO().getUserShortNumCount();
            } else {
                List<JSONObject> operatorCallDetail = this.mongoHandler.getUserOperatorCallDetail(request);
//                JSONObject operationReport = this.mongoHandler.getUserOperatorReport(request);
                List<JSONObject> contacts = this.mongoHandler.getUserDeviceContact(request);
                List<JSONObject> callRecords = this.mongoHandler.getUserDeviceCallRecord(request);
                Set<String> phones = new HashSet<>(); // 存放所有短号号码，

                if (null != operatorCallDetail && operatorCallDetail.size() > 0) {
                    for (JSONObject jsonObject : operatorCallDetail) {
                        String peerNumber = jsonObject.getString("peer_number");//通话手机号码
                        if ("110".equals(peerNumber)) {
                            continue;
                        }
                        if (StringUtils.isNotBlank(peerNumber) && peerNumber.length() == 3) {
                            phones.add(peerNumber);
                        }
                    }
                }

                if (null != contacts && contacts.size() > 0) {
                    for (JSONObject json : contacts) {
                        String phone = json.getString("contactsPhone");
                        phone = PhoneUtils.cleanTel(phone);
                        if ("110".equals(phone)) {
                            continue;
                        }
                        if (StringUtils.isNotBlank(phone) && phone.length() == 3) {
                            phones.add(phone);
                        }
                    }
                }

                if (null != callRecords && callRecords.size() > 0) {
                    for (JSONObject json : callRecords) {
                        String phone = json.getString("contactsPhone");
                        if ("110".equals(phone)) {
                            continue;
                        }
                        if (StringUtils.isNotBlank(phone) && phone.length() == 3) {
                            phones.add(phone);
                        }
                    }
                }

                // 排除特殊短号
                AdmissionRule rule = admissionRuleDao.getByRuleId(1037L);
                String setting = rule.getSetting();
                JSONObject sett = JSONObject.parseObject(setting);
                List<String> list = Arrays.asList(sett.getString("shortNos").split(","));
                phones.removeAll(list);
                count = phones.size();
            }
        } catch (Exception e) {
            log.error("模型：运营商通话记录验证黑名单异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 手机号码使用时间
     *
     * @param request
     * @return
     */
    public Integer robotOperatorPhoneUsedTime(DecisionHandleRequest request) {
        int count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserOpertorPhoneUsedTime()) {
                count = request.getRobotRequestDTO().getUserOpertorPhoneUsedTime();
            } else {
//                JSONObject operatorReport = this.mongoHandler.getUserOperatorReport(request);
                JSONObject operatorReport = this.mongoHandler.getOperatorInfo(request);

                if (null != operatorReport && !operatorReport.containsKey("open_time")) {
                    String openTimeStr = operatorReport.getString("open_time");
                    if (StringUtils.isBlank(openTimeStr)) {
                        JSONArray array = operatorReport.getJSONArray("bills");
                        count = null == array ? 0 : array.size();
                    } else {
                        Date openTime = DateTools.convert(openTimeStr);
                        count = DateConvert.getMonthDiff(new Date(), openTime);
                    }
                }
            }
        } catch (Exception e) {
            log.error("模型：手机号码使用时间异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 运营商平均话费验证（分）
     *
     * @param request
     * @return
     */
    public Integer robotOperatorAvgCharge(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserOperatorAvgCharge()) {
                count = request.getRobotRequestDTO().getUserOperatorAvgCharge();
            } else {
                JSONObject opertorInfo = this.mongoHandler.getOperatorInfo(request);
                BigDecimal averageFare = null == opertorInfo.get("averageFare") ? BigDecimal.ZERO : opertorInfo.getBigDecimal("averageFare");
                count = averageFare.multiply(new BigDecimal(100)).intValue();
            }
        } catch (Exception e) {
            log.error("模型：运营商平均话费验证（分）异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 树美多头借贷
     *
     * @param request
     * @return
     */
    public Integer robotShumeiMultiCount(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserShumeiCount()) {
                count = request.getRobotRequestDTO().getUserShumeiCount();
            } else {
                JSONObject rs = this.mongoHandler.getShumeiMultipoint(request);
                if (null != rs && null != rs.get("detail") && null != rs.getJSONObject("detail").get("itfin_loan_applications_7d")) {
                    count = rs.getJSONObject("detail").getInteger("itfin_loan_applications_7d");
                }
            }
        } catch (Exception e) {
            log.error("模型：树美多头借贷异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 10天内主叫次数-手机
     */
    public Integer robotCallNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallNum10() && 0 != request.getRobotRequestDTO().getUserCallNum10()) {
                return request.getRobotRequestDTO().getUserCallNum10();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_10, CALL);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallNum10(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallTime10(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：10天内主叫次数-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 10天内主叫时长-手机
     */
    public Integer robotCallTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallTime10() && 0 != request.getRobotRequestDTO().getUserCallTime10()) {
                return request.getRobotRequestDTO().getUserCallTime10();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_10, CALL);
            if (null == map || null == map.get("callTime")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallNum10(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallTime10(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callTime")));
            }
        } catch (Exception e) {
            log.error("模型：10天内主叫时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 10天内被叫次数-手机
     */
    public Integer robotCalledNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCalledNum10() && 0 != request.getRobotRequestDTO().getUserCalledNum10()) {
                return request.getRobotRequestDTO().getUserCalledNum10();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_10, CALLED);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCalledNum10(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCalledTime10(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：10天内被叫次数-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 10天内被叫时长-手机
     */
    public Integer robotCalledTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCalledTime10() && 0 != request.getRobotRequestDTO().getUserCalledTime10()) {
                return request.getRobotRequestDTO().getUserCalledTime10();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_10, CALLED);
            if (null == map || null == map.get("callTime")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCalledNum10(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCalledTime10(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callTime")));
            }
        } catch (Exception e) {
            log.error("模型：10天内被叫时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 10天内互通次数-手机
     *
     * @return
     */
    public Integer robotCallAndCalledNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallAndCalledNum10() && 0 != request.getRobotRequestDTO().getUserCallAndCalledNum10()) {
                return request.getRobotRequestDTO().getUserCallAndCalledNum10();
            }
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), DAY_10);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallAndCalledNum10(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallAndCalledTime10(Integer.valueOf(String.valueOf(map.get("callTime"))));
                request.getRobotRequestDTO().setUserCallAndCalledContactNum10(Integer.valueOf(String.valueOf(map.get("contactNum"))));
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：10天内互通次数-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 10天内互通时长-手机
     */
    public Integer robotCallAndCalledTime10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallAndCalledTime10() && 0 != request.getRobotRequestDTO().getUserCallAndCalledTime10()) {
                return request.getRobotRequestDTO().getUserCallAndCalledTime10();
            }
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), DAY_10);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallAndCalledNum10(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallAndCalledTime10(Integer.valueOf(String.valueOf(map.get("callTime"))));
                request.getRobotRequestDTO().setUserCallAndCalledContactNum10(Integer.valueOf(String.valueOf(map.get("contactNum"))));
                return Integer.valueOf(String.valueOf(map.get("callTime")));
            }
        } catch (Exception e) {
            log.error("模型：10天内互通时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 10天内互通人次-手机
     */
    public Integer robotCallAndCalledContactNum10(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallAndCalledContactNum10() && 0 != request.getRobotRequestDTO().getUserCallAndCalledContactNum10()) {
                return request.getRobotRequestDTO().getUserCallAndCalledContactNum10();
            }
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), DAY_10);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallAndCalledNum10(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallAndCalledTime10(Integer.valueOf(String.valueOf(map.get("callTime"))));
                request.getRobotRequestDTO().setUserCallAndCalledContactNum10(Integer.valueOf(String.valueOf(map.get("contactNum"))));
                return Integer.valueOf(String.valueOf(map.get("contactNum")));
            }
        } catch (Exception e) {
            log.error("模型：10天内互通人次-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 10天内通话时长和次数比值-手机
     */
    public Object robotCallAndCalledPercent10(DecisionHandleRequest request) {
        Object count = 0;
        try {
            if (null == request.getRobotRequestDTO().getUserCallNum10() || 0 == request.getRobotRequestDTO().getUserCallNum10()
                    || null == request.getRobotRequestDTO().getUserCallTime10() || 0 == request.getRobotRequestDTO().getUserCallTime10()) {

                Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_10, CALL);
                if (null == map) {
                    request.getRobotRequestDTO().setUserCallNum10(0);
                    request.getRobotRequestDTO().setUserCallTime10(0);
                } else {
                    request.getRobotRequestDTO().setUserCallNum10(Integer.valueOf(String.valueOf(map.get("callNum"))));
                    request.getRobotRequestDTO().setUserCallTime10(Integer.valueOf(String.valueOf(map.get("callTime"))));
                }
            }

            if (null == request.getRobotRequestDTO().getUserCalledNum10() || 0 == request.getRobotRequestDTO().getUserCalledNum10()
                    || null == request.getRobotRequestDTO().getUserCalledTime10() || 0 == request.getRobotRequestDTO().getUserCalledTime10()) {

                Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_10, CALLED);
                if (null == map) {
                    request.getRobotRequestDTO().setUserCalledNum10(0);
                    request.getRobotRequestDTO().setUserCalledTime10(0);
                } else {
                    request.getRobotRequestDTO().setUserCalledNum10(Integer.valueOf(String.valueOf(map.get("callNum"))));
                    request.getRobotRequestDTO().setUserCalledTime10(Integer.valueOf(String.valueOf(map.get("callTime"))));
                }
            }

            Integer allCallnum = request.getRobotRequestDTO().getUserCalledNum10() + request.getRobotRequestDTO().getUserCallNum10();
            Integer allCallTime = request.getRobotRequestDTO().getUserCalledTime10() + request.getRobotRequestDTO().getUserCallTime10();

            if (null == allCallnum || 0 == allCallnum || null == allCallTime || 0 == allCallTime) {
                return count;
            }

            count = new BigDecimal(allCallTime).divide(new BigDecimal(allCallnum), 4, BigDecimal.ROUND_DOWN);

        } catch (Exception e) {
            log.error("模型：10天内通话时长和次数比值-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }


    /**
     * 30天内主叫次数-手机
     */
    public Integer robotCallNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallNum30() && 0 != request.getRobotRequestDTO().getUserCallNum30()) {
                return request.getRobotRequestDTO().getUserCallNum30();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_30, CALL);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallNum30(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallTime30(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：30天内主叫次数-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 30天内主叫时长-手机
     */
    public Integer robotCallTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallTime30() && 0 != request.getRobotRequestDTO().getUserCallTime30()) {
                return request.getRobotRequestDTO().getUserCallTime30();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_30, CALL);
            if (null == map || null == map.get("callTime")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallNum30(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallTime30(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callTime")));
            }
        } catch (Exception e) {
            log.error("模型：30天内主叫时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 30天内被叫次数-手机
     */
    public Integer robotCalledNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCalledNum30() && 0 != request.getRobotRequestDTO().getUserCalledNum30()) {
                return request.getRobotRequestDTO().getUserCalledNum30();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_30, CALLED);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCalledNum30(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCalledTime30(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：30天内被叫次数-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 30天内被叫时长-手机
     */
    public Integer robotCalledTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCalledTime30() && 0 != request.getRobotRequestDTO().getUserCalledTime30()) {
                return request.getRobotRequestDTO().getUserCalledTime30();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_30, CALLED);
            if (null == map || null == map.get("callTime")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCalledNum30(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCalledTime30(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callTime")));
            }
        } catch (Exception e) {
            log.error("模型：30天内被叫时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 30天内互通次数-手机
     *
     * @return
     */
    public Integer robotCallAndCalledNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallAndCalledNum30() && 0 != request.getRobotRequestDTO().getUserCallAndCalledNum30()) {
                return request.getRobotRequestDTO().getUserCallAndCalledNum30();
            }
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), DAY_30);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallAndCalledNum30(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallAndCalledTime30(Integer.valueOf(String.valueOf(map.get("callTime"))));
                request.getRobotRequestDTO().setUserCallAndCalledContactNum30(Integer.valueOf(String.valueOf(map.get("contactNum"))));
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：30天内互通次数-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 30天内互通时长-手机
     */
    public Integer robotCallAndCalledTime30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallAndCalledTime30() && 0 != request.getRobotRequestDTO().getUserCallAndCalledTime30()) {
                return request.getRobotRequestDTO().getUserCallAndCalledTime30();
            }
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), DAY_30);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallAndCalledNum30(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallAndCalledTime30(Integer.valueOf(String.valueOf(map.get("callTime"))));
                request.getRobotRequestDTO().setUserCallAndCalledContactNum30(Integer.valueOf(String.valueOf(map.get("contactNum"))));
                return Integer.valueOf(String.valueOf(map.get("callTime")));
            }
        } catch (Exception e) {
            log.error("模型：30天内互通时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 30天内互通人次-手机
     */
    public Integer robotCallAndCalledContactNum30(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallAndCalledContactNum30() && 0 != request.getRobotRequestDTO().getUserCallAndCalledContactNum30()) {
                return request.getRobotRequestDTO().getUserCallAndCalledContactNum30();
            }
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), DAY_30);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallAndCalledNum30(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallAndCalledTime30(Integer.valueOf(String.valueOf(map.get("callTime"))));
                request.getRobotRequestDTO().setUserCallAndCalledContactNum30(Integer.valueOf(String.valueOf(map.get("contactNum"))));
                return Integer.valueOf(String.valueOf(map.get("contactNum")));
            }
        } catch (Exception e) {
            log.error("模型：30天内互通人次-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 30天内通话时长和次数比值-手机
     */
    public Object robotCallAndCalledPercent30(DecisionHandleRequest request) {
        Object count = 0;
        try {
            if (null == request.getRobotRequestDTO().getUserCallNum30() || 0 == request.getRobotRequestDTO().getUserCallNum30()
                    || null == request.getRobotRequestDTO().getUserCallTime30() || 0 == request.getRobotRequestDTO().getUserCallTime30()) {

                Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_30, CALL);
                if (null == map) {
                    request.getRobotRequestDTO().setUserCallNum30(0);
                    request.getRobotRequestDTO().setUserCallTime30(0);
                } else {
                    request.getRobotRequestDTO().setUserCallNum30(Integer.valueOf(String.valueOf(map.get("callNum"))));
                    request.getRobotRequestDTO().setUserCallTime30(Integer.valueOf(String.valueOf(map.get("callTime"))));
                }
            }

            if (null == request.getRobotRequestDTO().getUserCalledNum30() || 0 == request.getRobotRequestDTO().getUserCalledNum30()
                    || null == request.getRobotRequestDTO().getUserCalledTime30() || 0 == request.getRobotRequestDTO().getUserCalledTime30()) {

                Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_30, CALLED);
                if (null == map) {
                    request.getRobotRequestDTO().setUserCalledNum30(0);
                    request.getRobotRequestDTO().setUserCalledTime30(0);
                } else {
                    request.getRobotRequestDTO().setUserCalledNum30(Integer.valueOf(String.valueOf(map.get("callNum"))));
                    request.getRobotRequestDTO().setUserCalledTime30(Integer.valueOf(String.valueOf(map.get("callTime"))));
                }
            }

            Integer allCallnum = request.getRobotRequestDTO().getUserCalledNum30() + request.getRobotRequestDTO().getUserCallNum30();
            Integer allCallTime = request.getRobotRequestDTO().getUserCalledTime30() + request.getRobotRequestDTO().getUserCallTime30();

            if (null == allCallnum || 0 == allCallnum || null == allCallTime || 0 == allCallTime) {
                return count;
            }

            count = new BigDecimal(allCallTime).divide(new BigDecimal(allCallnum), 4, BigDecimal.ROUND_DOWN);

        } catch (Exception e) {
            log.error("模型：30天内通话时长和次数比值-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }


    /**
     * 60天内主叫次数-手机
     */
    public Integer robotCallNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallNum60() && 0 != request.getRobotRequestDTO().getUserCallNum60()) {
                return request.getRobotRequestDTO().getUserCallNum60();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_60, CALL);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallNum60(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallTime60(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：60天内主叫次数-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 60天内主叫时长-手机
     */
    public Integer robotCallTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallTime60() && 0 != request.getRobotRequestDTO().getUserCallTime60()) {
                return request.getRobotRequestDTO().getUserCallTime60();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_60, CALL);
            if (null == map || null == map.get("callTime")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallNum60(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallTime60(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callTime")));
            }
        } catch (Exception e) {
            log.error("模型：60天内主叫时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 60天内被叫次数-手机
     */
    public Integer robotCalledNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCalledNum60() && 0 != request.getRobotRequestDTO().getUserCalledNum60()) {
                return request.getRobotRequestDTO().getUserCalledNum60();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_60, CALLED);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCalledNum60(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCalledTime60(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：60天内被叫次数-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 60天内被叫时长-手机
     */
    public Integer robotCalledTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCalledTime60() && 0 != request.getRobotRequestDTO().getUserCalledTime60()) {
                return request.getRobotRequestDTO().getUserCalledTime60();
            }
            Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_60, CALLED);
            if (null == map || null == map.get("callTime")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCalledNum60(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCalledTime60(Integer.valueOf(String.valueOf(map.get("callTime"))));
                return Integer.valueOf(String.valueOf(map.get("callTime")));
            }
        } catch (Exception e) {
            log.error("模型：60天内被叫时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 60天内互通次数-手机
     *
     * @return
     */
    public Integer robotCallAndCalledNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallAndCalledNum60() && 0 != request.getRobotRequestDTO().getUserCallAndCalledNum60()) {
                return request.getRobotRequestDTO().getUserCallAndCalledNum60();
            }
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), DAY_60);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallAndCalledNum60(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallAndCalledTime60(Integer.valueOf(String.valueOf(map.get("callTime"))));
                request.getRobotRequestDTO().setUserCallAndCalledContactNum60(Integer.valueOf(String.valueOf(map.get("contactNum"))));
                return Integer.valueOf(String.valueOf(map.get("callNum")));
            }
        } catch (Exception e) {
            log.error("模型：60天内互通次数-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 60天内互通时长-手机
     */
    public Integer robotCallAndCalledTime60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallAndCalledTime60() && 0 != request.getRobotRequestDTO().getUserCallAndCalledTime60()) {
                return request.getRobotRequestDTO().getUserCallAndCalledTime60();
            }
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), DAY_60);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallAndCalledNum60(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallAndCalledTime60(Integer.valueOf(String.valueOf(map.get("callTime"))));
                request.getRobotRequestDTO().setUserCallAndCalledContactNum60(Integer.valueOf(String.valueOf(map.get("contactNum"))));
                return Integer.valueOf(String.valueOf(map.get("callTime")));
            }
        } catch (Exception e) {
            log.error("模型：60天内互通时长-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 60天内互通人次-手机
     */
    public Integer robotCallAndCalledContactNum60(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            if (null != request.getRobotRequestDTO().getUserCallAndCalledContactNum60() && 0 != request.getRobotRequestDTO().getUserCallAndCalledContactNum60()) {
                return request.getRobotRequestDTO().getUserCallAndCalledContactNum60();
            }
            Map<String, Object> map = this.modelService.getCallAndCalledByDay(request.getNid(), request.getApplyTime(), DAY_60);
            if (null == map || null == map.get("callNum")) {
                return count;
            } else {
                request.getRobotRequestDTO().setUserCallAndCalledNum60(Integer.valueOf(String.valueOf(map.get("callNum"))));
                request.getRobotRequestDTO().setUserCallAndCalledTime60(Integer.valueOf(String.valueOf(map.get("callTime"))));
                request.getRobotRequestDTO().setUserCallAndCalledContactNum60(Integer.valueOf(String.valueOf(map.get("contactNum"))));
                return Integer.valueOf(String.valueOf(map.get("contactNum")));
            }
        } catch (Exception e) {
            log.error("模型：60天内互通人次-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     * 60天内通话时长和次数比值-手机
     */
    public Object robotCallAndCalledPercent60(DecisionHandleRequest request) {
        Object count = 0;
        try {
            if (null == request.getRobotRequestDTO().getUserCallNum60() || 0 == request.getRobotRequestDTO().getUserCallNum60()
                    || null == request.getRobotRequestDTO().getUserCallTime60() || 0 == request.getRobotRequestDTO().getUserCallTime60()) {

                Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_60, CALL);
                if (null == map) {
                    request.getRobotRequestDTO().setUserCallNum60(0);
                    request.getRobotRequestDTO().setUserCallTime60(0);
                } else {
                    request.getRobotRequestDTO().setUserCallNum60(Integer.valueOf(String.valueOf(map.get("callNum"))));
                    request.getRobotRequestDTO().setUserCallTime60(Integer.valueOf(String.valueOf(map.get("callTime"))));
                }
            }

            if (null == request.getRobotRequestDTO().getUserCalledNum60() || 0 == request.getRobotRequestDTO().getUserCalledNum60()
                    || null == request.getRobotRequestDTO().getUserCalledTime60() || 0 == request.getRobotRequestDTO().getUserCalledTime60()) {

                Map<String, Object> map = this.modelService.getCallNumByDay(request.getNid(), request.getApplyTime(), DAY_60, CALLED);
                if (null == map) {
                    request.getRobotRequestDTO().setUserCalledNum60(0);
                    request.getRobotRequestDTO().setUserCalledTime60(0);
                } else {
                    request.getRobotRequestDTO().setUserCalledNum60(Integer.valueOf(String.valueOf(map.get("callNum"))));
                    request.getRobotRequestDTO().setUserCalledTime60(Integer.valueOf(String.valueOf(map.get("callTime"))));
                }
            }

            Integer allCallnum = request.getRobotRequestDTO().getUserCalledNum60() + request.getRobotRequestDTO().getUserCallNum60();
            Integer allCallTime = request.getRobotRequestDTO().getUserCalledTime60() + request.getRobotRequestDTO().getUserCallTime60();

            if (null == allCallnum || 0 == allCallnum || null == allCallTime || 0 == allCallTime) {
                return count;
            }

            count = new BigDecimal(allCallTime).divide(new BigDecimal(allCallnum), 4, BigDecimal.ROUND_DOWN);

        } catch (Exception e) {
            log.error("模型：60天内通话时长和次数比值-手机异常，nid;{},error", request.getNid(), e);
        }
        return count;
    }

    /**
     *  通话风险分析-与催收类号码联系情况次数（3个月总次数）
     */
    public Integer robotCallRiskAnalysisCollection(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
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
            log.error("模型：通话风险分析-与催收类号码联系情况次数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-与信用卡号码联系情况次数（3个月总次数）
     */
    public Integer robotCallRiskAnalysisCreditCard(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
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
            log.error("模型：通话风险分析-与信用卡类号码联系情况次数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-与贷款类号码联系情况次数（3个月总次数）
     */
    public Integer robotCallRiskAnalysisLoan(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
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
            log.error("模型：通话风险分析-与贷款类号码联系情况次数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-110，120，律师、法院等通话次数（3个月总次数）
     */
    public Integer robotCallRiskAnalysisGov(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
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
            log.error("模型：通话风险分析-与110，120，律师、法院等次数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-用户号码联系黑中介分数（分数范围0-100，参考分为10，分数越低关系越紧密
     */
    public Integer robotCallCheckBlackInfoScore(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return null;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_BLACK_INFO.getValue());
            if (null == itemJson) {
                return null;
            }
            count = itemJson.getInteger("phone_gray_score");
        } catch (Exception e) {
            log.error("模型：通话风险分析-联系黑中介分数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-引起间接黑名单人数
     */
    public Integer robotCallCheckBlackInfoRouter(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return null;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_BLACK_INFO.getValue());
            if (null == itemJson) {
                return null;
            }
            count = itemJson.getInteger("contacts_router_cnt");
        } catch (Exception e) {
            log.error("模型：通话风险分析-引起间接黑名单人数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-间接联系人中黑名单人数
     */
    public Integer robotCallCheckBlackInfoClass2Cnt(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return null;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_BLACK_INFO.getValue());
            if (null == itemJson) {
                return null;
            }
            count = itemJson.getInteger("contacts_class2_blacklist_cnt");
        } catch (Exception e) {
            log.error("模型：通话风险分析-间接联系人中黑名单人数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-查询过该用户的相关企业数量（姓名+身份证+电话号码）
     */
    public Integer robotCallSearchedOrgCnt(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return null;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return null;
            }
            count = itemJson.getInteger("searched_org_cnt");
        } catch (Exception e) {
            log.error("模型：通话风险分析-查询过该用户的相关企业数量，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-身份证组合过的其他姓名(返回了匹配数量)
     */
    public Integer robotCallIdcardWithOtherNames(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return null;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return null;
            }
            JSONArray array = itemJson.getJSONArray("idcard_with_other_names");
            if (null == array) {
                return null;
            }
            count = array.size();
        } catch (Exception e) {
            log.error("模型：通话风险分析-身份证组合过的其他姓名，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-身份证组合过其他电话(返回了匹配数量)
     */
    public Integer robotCallIdcardWithOtherPhones(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return null;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return null;
            }
            JSONArray array = itemJson.getJSONArray("idcard_with_other_phones");
            if (null == array) {
                return null;
            }
            count = array.size();
        } catch (Exception e) {
            log.error("模型：通话风险分析-身份证组合过其他电话，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-电话号码组合过其他姓名(返回了匹配数量)
     */
    public Integer robotCallPhoneWithOtherNames(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return null;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return null;
            }
            JSONArray array = itemJson.getJSONArray("phone_with_other_names");
            if (null == array) {
                return null;
            }
            count = array.size();
        } catch (Exception e) {
            log.error("模型：通话风险分析-身份证组合过其他电话，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  通话风险分析-电话号码组合过其他身份证(返回了匹配数量)
     */
    public Integer robotCallPhoneWithOtherIdcards(DecisionHandleRequest request) {
        Integer count = 0;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray userInfoCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.USER_INFO_CHECK.getValue());
            if (null == userInfoCheck || userInfoCheck.size() == 0) {
                return null;
            }

            JSONObject itemJson = ((JSONObject) userInfoCheck.get(0)).getJSONObject(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CHECK_SEARCH_INFO.getValue());
            if (null == itemJson) {
                return null;
            }
            JSONArray array = itemJson.getJSONArray("phone_with_other_idcards");
            if (null == array) {
                return null;
            }
            count = array.size();
        } catch (Exception e) {
            log.error("模型：通话风险分析-电话号码组合过其他身份证，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  出行分析-联系人所在区域个数汇总
     */
    public Integer robotCallContactRegion(DecisionHandleRequest request) {
        Integer count = null;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray contactRegion = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CONTACT_REGION.getValue());
            if (null == contactRegion || contactRegion.size() == 0) {
                return null;
            }

            for (Object item : contactRegion) {
                JSONObject itemJson = (JSONObject) item;
                String key = itemJson.getString("key");
                if (null == key && key.equals("contact_region_6m")) {
                    if (null == itemJson.getJSONArray("region_list")) {
                        return null;
                    }
                    count = itemJson.getJSONArray("region_list").size();
                }
            }
        } catch (Exception e) {
            log.error("模型：出行分析-联系人所在区域个数汇总，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  出行分析（外出不同城市的个数，曾在那些城市打过电话）
     */
    public Integer robotCallTripInfo(DecisionHandleRequest request) {
        Integer count = null;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.TRIP_INFO.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return null;
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
            log.error("模型：出行分析-外出不同城市的个数，曾在那些城市打过电话，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  深夜[1:30-5:30]通话总次数
     */
    public Integer robotCallDurationDetail(DecisionHandleRequest request) {
        Integer count = null;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return null;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;

                if (StringUtils.isNotEmpty(itemJson.getString("time_step"))
                        && !itemJson.getString("time_step").equalsIgnoreCase("time_step")) {
                    JSONObject detail = itemJson.getJSONObject("item");
                    count = detail.getInteger("total_cnt");
                }
            }
        } catch (Exception e) {
            log.error("模型：通话分析-深夜[1:30-5:30]通话总次数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  深夜[1:30-5:30]通话数
     */
    public Integer robotCallMidnightTotalCnt(DecisionHandleRequest request) {
        Integer count = null;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return null;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;

                if (StringUtils.isNotEmpty(itemJson.getString("time_step"))
                        && !itemJson.getString("time_step").equalsIgnoreCase("midnight")) {
                    JSONObject detail = itemJson.getJSONObject("item");
                    count = detail.getInteger("total_cnt");
                }
            }
        } catch (Exception e) {
            log.error("模型：深夜通话分析-深夜[1:30-5:30]通话总次数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  深夜[1:30-5:30]通话号码数
     */
    public Integer robotCallMidnightUniqNumCnt(DecisionHandleRequest request) {
        Integer count = null;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return null;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;

                if (StringUtils.isNotEmpty(itemJson.getString("time_step"))
                        && !itemJson.getString("time_step").equalsIgnoreCase("midnight")) {
                    JSONObject detail = itemJson.getJSONObject("item");
                    count = detail.getInteger("uniq_num_cnt");
                }
            }
        } catch (Exception e) {
            log.error("模型：深夜通话分析-深夜[1:30-5:30]通话号码数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  深夜[1:30-5:30]主叫数
     */
    public Integer robotCallMidnightDialCnt(DecisionHandleRequest request) {
        Integer count = null;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return null;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;

                if (StringUtils.isNotEmpty(itemJson.getString("time_step"))
                        && !itemJson.getString("time_step").equalsIgnoreCase("midnight")) {
                    JSONObject detail = itemJson.getJSONObject("item");
                    count = detail.getInteger("dial_cnt");
                }
            }
        } catch (Exception e) {
            log.error("模型：深夜通话分析-深夜[1:30-5:30]主叫数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  深夜[1:30-5:30]被叫数
     */
    public Integer robotCallMidnightDialedCnt(DecisionHandleRequest request) {
        Integer count = null;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CALL_DURATION_DETAIL.getValue());
            if (null == tripInfo || tripInfo.size() == 0) {
                return null;
            }

            for (Object item : tripInfo) {
                JSONObject itemJson = (JSONObject) item;

                if (StringUtils.isNotEmpty(itemJson.getString("time_step"))
                        && !itemJson.getString("time_step").equalsIgnoreCase("midnight")) {
                    JSONObject detail = itemJson.getJSONObject("item");
                    count = detail.getInteger("dialed_cnt");
                }
            }
        } catch (Exception e) {
            log.error("模型：深夜通话分析-深夜[1:30-5:30]被叫数，nid;{},error", request.getNid(), e);
        }

        return count;
    }

    /**
     *  运营商-平均话费
     */
    public Double robotCallUserOperatorAvgCharge(DecisionHandleRequest request) {
        Double avgCharge = null;
        try {
            JSONObject operatorInfo = request.getRobotRequestDTO().getOperatorInfo();

            JSONArray bills = operatorInfo.getJSONArray("bills");
            List<Integer> arr = new ArrayList<>();
            if (null != bills && !bills.isEmpty()) {
                for (Object bill : bills) {
                    JSONObject itemJson = (JSONObject) bill;
                    arr.add(((JSONObject) bill).getIntValue("actual_fee"));
                }
                Collections.sort(arr);
            }
            String result = Average.getAverages(arr);
            avgCharge = Double.valueOf(result);
        } catch (Exception e) {
            log.error("模型：运营商-平均话费，nid;{},error", request.getNid(), e);
        }
        return avgCharge;
    }

    /**
     *  行为分析-手机静默情况
     */
    public Integer robotCallPhoneSilent(DecisionHandleRequest request) {
        Integer count = null;
        try {
            JSONObject operatorReport = request.getRobotRequestDTO().getOperatorReport();
            JSONArray behaviorCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.BEHAVIOR_CHECK.getValue());
            if (null == behaviorCheck || behaviorCheck.size() == 0) {
                return null;
            }

            Set<String> areas = new HashSet<>();

            for (Object item : behaviorCheck) {
                JSONObject itemJson = (JSONObject) item;

                if (StringUtils.isNotEmpty(itemJson.getString("check_point"))
                        && !itemJson.getString("check_point").equalsIgnoreCase("phone_silent")) {
                    count = itemJson.getInteger("score");
                }
            }
        } catch (Exception e) {
            log.error("模型：行为分析-手机静默情况，nid;{},error", request.getNid(), e);
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

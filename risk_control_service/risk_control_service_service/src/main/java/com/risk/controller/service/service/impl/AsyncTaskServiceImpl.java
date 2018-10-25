package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.constans.ERROR;
import com.risk.controller.service.common.utils.ContextUtils;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.AdmissionResultDao;
import com.risk.controller.service.dao.DecisionReqLogDao;
import com.risk.controller.service.dao.DecisionRobotNoticeDao;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.dto.DecisionConfigDTO;
import com.risk.controller.service.dto.RobotScoreDTO;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.DecisionRobotService;
import com.risk.controller.service.service.DecisionRuleService;
import com.risk.controller.service.service.DecisionService;
import com.risk.controller.service.util.AdmissionHandler;
import com.risk.controller.service.utils.CSVUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 处理异步任务的业务层实现
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Service
@Slf4j
public class AsyncTaskServiceImpl {

    @Autowired
    private DecisionRobotService robotService;

    @Autowired
    private LocalCache localCache;

    @Autowired
    private DecisionRuleService decisionRuleService;

    @Autowired
    private DecisionRobotNoticeDao decisionRobotNoticeDao;

    @Autowired
    private AdmissionResultDao admissionResultDao;

    @Autowired
    private DecisionService decisionService;

    @Autowired
    private DecisionReqLogDao decisionReqLogDao;

    @Autowired
    private PaixuServiceImpl paixuServiceImpl;

    @Async
    public void asyncHandler(DecisionHandleRequest request, AdmissionResult admissionResult, Long labelGroupId, AdmissionResultDTO ret) {

        // 决策结果汇总
        AdmissionResultDTO admResult = this.handle(request, admissionResult, labelGroupId, ret);

        // 如果是挂起，或者异常，重跑决策
        if (AdmissionResultDTO.RESULT_SUSPEND == admResult.getResult() || AdmissionResultDTO.RESULT_EXCEPTIONAL == admResult.getResult()) {
            return;
        }

        // 通过,拒绝,人工审核 通知业务端
        else if (AdmissionResultDTO.RESULT_APPROVED == admResult.getResult()
                || AdmissionResultDTO.RESULT_REJECTED == admResult.getResult()
                || AdmissionResultDTO.RESULT_MANUAL == admResult.getResult()) {
            decisionService.noticeBorrowResultHandle(request.getNid(), admResult);
        }

        // 其他情况
        else {
            log.warn("决策返回结果异常admResult：{}", JSONObject.toJSONString(admResult));
        }
    }

    public AdmissionResultDTO handle(DecisionHandleRequest request, AdmissionResult admissionResult, Long labelGroupId, AdmissionResultDTO ret) {

        Integer ONE = 1;
        DecisionConfigDTO config = new DecisionConfigDTO();
        config.setFailFast(request.getFailFast());

        // 根据组id获取标签组信息
        DecisionLabelGroup labelGroup = this.decisionRuleService.getLabelGroupById(labelGroupId);

        // 步骤数
        int stageCount = (null == labelGroup.getStageCount()) ? 0 : labelGroup.getStageCount();

        try {
            // 快速失败
            if (ONE.equals(request.getFailFast())) {

                // 分阶段执行规则, 从1开始
                for (int stage = 1; stage <= stageCount; stage++) {
                    admissionResult.setStopStage(stage);
                    // 根据组id和步骤获取执行规则集
                    List<AdmissionRule> ruleList = this.decisionRuleService.getAdmissionRule(labelGroupId, stage);  // 要执行的规则集

                    if (null == ruleList || ruleList.isEmpty()) {
                        continue;
                    }
                    AdmissionResultDTO stageResult = this.handle(config, stage, request, ruleList);
                    ret.getRejectReason().addAll(stageResult.getRejectReason());

                    // 保存决策明细
                    decisionRuleService.saveAdmissionResult(admissionResult.getId(), stageResult.getResultDetail());

                    int stageResultStatus = stageResult.getResult();

                    // 挂起和异常结束
                    if (stageResultStatus == AdmissionResultDTO.RESULT_SUSPEND
                            || stageResultStatus == AdmissionResultDTO.RESULT_EXCEPTIONAL) {

                        admissionResult.setSuspendStage(stage);
                        ret.setSuspendDetail(stageResult.getSuspendDetail());
                        ret.setResult(stageResultStatus);
                        break;
                    }
                    // 拒绝结束
                    else if (stageResultStatus == AdmissionResultDTO.RESULT_REJECTED) {
                        ret.setResult(stageResultStatus);
                        break;
                    }
                    // 人工审核则设置最终结果为人工审核
                    else if (stageResultStatus == AdmissionResultDTO.RESULT_MANUAL) {
                        ret.setResult(stageResultStatus);
                    }

                }
            } else { // 非快速失败
                int approvedCount = 0;
                int rejectedCount = 0;
                int exceptionalCount = 0;
                int finalApprovedCount = 0;
                int manualCount = 0;
                int suspentCount = 0;
                for (int stage = 1; stage <= stageCount; stage++) { // 分阶段执行规则, 从1开始
                    admissionResult.setStopStage(stage);
                    List<AdmissionRule> ruleList = this.decisionRuleService.getAdmissionRule(labelGroupId, stage);  // 要执行的规则集
                    if (null == ruleList || ruleList.isEmpty()) {
                        continue;
                    }

                    AdmissionResultDTO stageResult = this.handle(config, stage, request, ruleList);
                    ret.getRejectReason().addAll(stageResult.getRejectReason());
                    approvedCount += stageResult.getApprovedCount();
                    rejectedCount += stageResult.getRejectedCount();
                    exceptionalCount += stageResult.getExceptionalCount();
                    finalApprovedCount += stageResult.getFinalApprovedCount();
                    manualCount += stageResult.getManualCount();
                    suspentCount += stageResult.getSuspendCount();

                    int stageRobotAction = (stageResult.getRobotAction() == null) ? AdmissionResultDTO.ROBOT_ACTION_SCORE : stageResult.getRobotAction();
                    if (AdmissionResultDTO.ROBOT_ACTION_SKIP == stageRobotAction) {
                        ret.setRobotAction(AdmissionResultDTO.ROBOT_ACTION_SKIP);
                    }

                    if (stageResult.getSuspendCount() > 0) {
                        admissionResult.setSuspendStage(stage);
                        ret.setSuspendDetail(stageResult.getSuspendDetail());
                    }

                    // insert admission result detail
                    this.decisionRuleService.saveAdmissionResult(admissionResult.getId(), stageResult.getResultDetail());
                }

                ret.setApprovedCount(approvedCount);
                ret.setRejectedCount(rejectedCount);
                ret.setExceptionalCount(exceptionalCount);
                ret.setFinalApprovedCount(finalApprovedCount);
                ret.setManualCount(manualCount);
                ret.setSuspendCount(suspentCount);
                ret.setResultByRuleResult();
            }
        } catch (Exception e) {
            ret.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
        }
        long endTime = System.currentTimeMillis();
        admissionResult.setResult(ret.getResult());
        admissionResult.setRobotAction(ret.getRobotAction());
        long preCost = (null == admissionResult.getTimeCost()) ? 0 : admissionResult.getTimeCost().longValue();
        admissionResult.setTimeCost(endTime - admissionResult.getSuspendTime() + preCost);

        if (ret.getResult() == AdmissionResultDTO.RESULT_SUSPEND || ret.getResult() == AdmissionResultDTO.RESULT_EXCEPTIONAL) {
            admissionResult.setSuspendCnt(admissionResult.getSuspendCnt() + 1);
            if (null != ret.getSuspendDetail()) {
                admissionResult.setSuspendRuleId(ret.getSuspendDetail().getRuleId());
                admissionResult.setSuspendTime(ret.getSuspendDetail().getSuspendTime());
            }
        }
        this.admissionResultDao.updateByPrimaryKeySelective(admissionResult);
        return ret;
    }

    private AdmissionResultDTO handle(DecisionConfigDTO config, int stage, DecisionHandleRequest request, List<AdmissionRule> ruleList) {

        AdmissionResultDTO ret = new AdmissionResultDTO();
        Set<String> rejectCodeSet = new LinkedHashSet<String>();
        List<AdmissionResultDetail> resultDetailList = new ArrayList<AdmissionResultDetail>();
        ret.setRejectReason(rejectCodeSet); // 信审拒绝原因拒绝原因表中对应的字段
        ret.setResultDetail(resultDetailList);
        ret.setResult(AdmissionResultDTO.RESULT_APPROVED);

        Integer ONE = 1;
        int approvedCount = 0;
        int rejectedCount = 0;
        int exceptionalCount = 0;
        int finalApprovedCount = 0;
        int manualCount = 0;
        int suspentCount = 0;

        if (null != ruleList && ruleList.size() > 0) {
            boolean failFast = ONE.equals(config.getFailFast());
            for (AdmissionRule rule : ruleList) {
                // 该规则是否是挂起规则/异常
                boolean isSupent = request.getResultDetailMap() == null ? false : request.getResultDetailMap().get(rule.getId()) == null ? false : (AdmissionResultDTO.RESULT_SUSPEND == request.getResultDetailMap().get(rule.getId()).getResult() || AdmissionResultDTO.RESULT_EXCEPTIONAL == request.getResultDetailMap().get(rule.getId()).getResult());
                // 该规则是否未执行过
                boolean isNoExec = request.getResultDetailMap() == null ? true : !request.getResultDetailMap().containsKey(rule.getId());

                AdmissionRuleDTO ruleDto = AdmissionRuleDTO.fromAdmissionRule(rule);
                String handler = ruleDto.getHandler();
                if (StringUtils.isEmpty(handler)) {
                    continue;
                }

                AdmissionResultDTO tmpResult = null;
                Set<String> tmpReasonCode = null;
                int tmpResultStatus = AdmissionResultDTO.RESULT_APPROVED;
                Object tmpData = null;
                int delimiterInd = handler.lastIndexOf('.');
                String className = handler.substring(0, delimiterInd); // 处理类
                String methodName = handler.substring(delimiterInd + 1); // 处理方法

                AdmissionHandler handlerObj = this.getHandler(className);
                Method methodObj = this.getHandlerMethod(handlerObj, methodName);

                // 执行未执行过的规则或挂起的规则
                AdmissionResultDetail resultDetail = null;
                if (isNoExec || isSupent) {
                    try {
                        if (isSupent) {
                            //挂起规则的执行结果
                            resultDetail = request.getResultDetailMap().get(rule.getId());

                            ruleDto.setSuspendCnt(resultDetail.getSuspendCnt());
                            ruleDto.setSuspendTime(resultDetail.getSuspendTime());

                        } else {
                            // 首次执行
                            resultDetail = new AdmissionResultDetail();
                            resultDetail.setSuspendTime(System.currentTimeMillis());
                            resultDetail.setSuspendCnt(0);
                            resultDetail.setTimeCost(0L);

                            resultDetail.setRuleId(rule.getId());
                            resultDetail.setStage(stage);
                            resultDetail.setSuspendStage(stage);

                            ruleDto.setSuspendTime(0L);
                            ruleDto.setSuspendCnt(0);
                        }

                        tmpResult = (AdmissionResultDTO) methodObj.invoke(handlerObj, request, ruleDto);

                        long endIndividualTime = System.currentTimeMillis();

                        if (tmpResult == null) {
                            tmpResultStatus = AdmissionResultDTO.RESULT_REJECTED;
                        } else {
                            tmpResultStatus = tmpResult.getResult();
                            tmpReasonCode = tmpResult.getRejectReason();
                            tmpData = tmpResult.getData();

                            int robotAction = (tmpResult.getRobotAction() == null) ? AdmissionResultDTO.ROBOT_ACTION_SCORE : tmpResult.getRobotAction();
                            if (AdmissionResultDTO.ROBOT_ACTION_SKIP == robotAction) {
                                ret.setRobotAction(AdmissionResultDTO.ROBOT_ACTION_SKIP);
                            }

                            // 检查挂起超限
                            if (tmpResultStatus == AdmissionResultDTO.RESULT_SUSPEND || tmpResultStatus == AdmissionResultDTO.RESULT_EXCEPTIONAL) {
                                if ((resultDetail.getSuspendCnt() + 1) > rule.getMaxSuspendCnt()) {
                                    tmpResultStatus = rule.getSuspendResult();
                                    tmpReasonCode = null;
                                    tmpData = null;
                                }

                                if ((endIndividualTime - resultDetail.getSuspendTime() + resultDetail.getTimeCost()) > (1000 * rule.getMaxSuspendTimeout())) {
                                    tmpResultStatus = rule.getSuspendResult();
                                    tmpReasonCode = null;
                                    tmpData = null;
                                }
                            }

                            resultDetail.setTimeCost(endIndividualTime - resultDetail.getSuspendTime() + resultDetail.getTimeCost());
                            resultDetail.setSuspendTime(System.currentTimeMillis());

                            if (tmpResultStatus == AdmissionResultDTO.RESULT_SUSPEND || tmpResultStatus == AdmissionResultDTO.RESULT_EXCEPTIONAL) {
                                int preCnt = (null == resultDetail.getSuspendCnt()) ? 0 : resultDetail.getSuspendCnt().intValue();
                                resultDetail.setSuspendCnt(preCnt + 1);
                            }
                        }
                    } catch (Exception e) {
                        log.error("决策执行异常：request：{},error:", JSONObject.toJSONString(request), e);
                        tmpResultStatus = AdmissionResultDTO.RESULT_EXCEPTIONAL;
                    }
                } else {
                    resultDetail = request.getResultDetailMap().get(rule.getId());
                    tmpResultStatus = resultDetail.getResult();
                    tmpReasonCode = new HashSet();
                    if (StringUtils.isNotEmpty(resultDetail.getRejectReasonCode())) {
                        tmpReasonCode.add(resultDetail.getRejectReasonCode());
                    }

                    tmpData = resultDetail.getData();
                }

                resultDetail.setResult(tmpResultStatus);

                if ((isSupent || isNoExec) && null != tmpData) {
                    String tmpDataString = JSON.toJSONString(tmpData);
                    if (tmpDataString.length() > 1003) {
                        tmpDataString = tmpDataString.substring(0, 1000) + "...";
                    }

                    resultDetail.setDataType(tmpData.getClass().getSimpleName());
                    resultDetail.setData(tmpDataString);
                }

                resultDetailList.add(resultDetail);
                ret.setSuspendDetail(resultDetail);

                // 所有规则都执行完，没有不通过的，最终才能下结论（终审通过）
                if (AdmissionResultDTO.RESULT_FINAL_APPROVED == tmpResultStatus) {
                    finalApprovedCount++;
                } else if (AdmissionResultDTO.RESULT_APPROVED == tmpResultStatus) {  // 继续执行
                    approvedCount++;
                } else if (AdmissionResultDTO.RESULT_REJECTED == tmpResultStatus) {  // 拒绝
                    rejectedCount++;
                    if (null != tmpReasonCode) {
                        rejectCodeSet.addAll(tmpReasonCode);
                    } else if (!StringUtils.isEmpty(rule.getRejectReasonCode())) {
                        String[] arrCode = rule.getRejectReasonCode().split(",");
                        for (String arrItem : arrCode) {
                            rejectCodeSet.add(arrItem);
                        }
                    }
                    // 只有 RESULT_REJECTED状态才能 break
                    if (failFast) {
                        break;
                    }
                } else if (AdmissionResultDTO.RESULT_EXCEPTIONAL == tmpResultStatus) {//异常
                    exceptionalCount++;
                    break;//订单挂起，暂停规则检查
                } else if (AdmissionResultDTO.RESULT_MANUAL == tmpResultStatus) {//人工审核
                    manualCount++;
                } else if (AdmissionResultDTO.RESULT_SUSPEND == tmpResultStatus) {//挂起
                    suspentCount++;
                    break;//订单挂起，暂停规则检查
                }
            }
        }

        ret.setApprovedCount(approvedCount);
        ret.setRejectedCount(rejectedCount);
        ret.setExceptionalCount(exceptionalCount);
        ret.setFinalApprovedCount(finalApprovedCount);
        ret.setManualCount(manualCount);
        ret.setSuspendCount(suspentCount);
        ret.setResultByRuleResult();
        return ret;
    }

    /**
     * spring IOC 获取bean（根据类型）
     *
     * @param className
     * @return
     */
    private AdmissionHandler getHandler(String className) {
        log.debug("enter method, className:{}", className);

        AdmissionHandler ret = null;
        if (!StringUtils.isEmpty(className)) {
            Class clazz = null;
            try {
                clazz = Class.forName(className);
                ret = (AdmissionHandler) ContextUtils.getApplicationContext().getBean(clazz);
            } catch (ClassNotFoundException e) {
                log.error("admission rule handler bean NOT found, className:{}", className);
                throw new RuntimeException(e);
            }
        }

        return ret;
    }

    private Method getHandlerMethod(AdmissionHandler handlerObj, String methodName) {
        log.debug("enter method, handlerObj:{}, methodName:{}", handlerObj, methodName);

        Method ret = null;
        try {
            ret = handlerObj.getClass().getDeclaredMethod(methodName, new Class[]{DecisionHandleRequest.class, AdmissionRuleDTO.class});
        } catch (Exception e) {
            log.error("admission rule handler NOT found, handlerObj:{}, methodName:{}", handlerObj, methodName);
            throw new RuntimeException(e);
        }

        return ret;
    }

}

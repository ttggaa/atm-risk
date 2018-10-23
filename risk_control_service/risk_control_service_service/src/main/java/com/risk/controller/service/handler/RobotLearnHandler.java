package com.risk.controller.service.handler;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.*;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.WanshuService;
import com.risk.controller.service.service.impl.LocalCache;
import com.risk.controller.service.util.AdmissionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

/**
 * 训练模型
 */
@Slf4j
@Service
public class RobotLearnHandler implements AdmissionHandler {

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
    private RobotResultDao robotResultDao;
    @Autowired
    private WanshuService wanshuService;
    @Autowired
    private DecisionReqLogDao decisionReqLogDao;

    /**
     * 训练模型入口
     *
     * @param list
     * @param bool true:累加，false从0开始计算
     */
    public void robotLearnDetail(List<Map<String, Object>> list, boolean bool) {

        if (null != list && list.size() > 0) {
            // 1.设置默认值
            List<RobotRule> ruleList = robotRuleDao.getAllrobotRule();
            if (!bool) {
                robotRuleDetailDao.updateAllSetZero();
            }
            List<RobotRuleDetail> ruleDetailsList = robotRuleDetailDao.getAllEnabled();

            this.setRobotLeanData(list, ruleList, ruleDetailsList);

            // 修改
            this.updateBatchRobotRuleDetail(ruleDetailsList);

        }
    }

    /**
     * 批量修改规则明细
     * @param ruleDetailsList
     */
    private void updateBatchRobotRuleDetail(List<RobotRuleDetail> ruleDetailsList) {
        if (null != ruleDetailsList && ruleDetailsList.size() > 0) {
            // 1、计算比值
            for (RobotRuleDetail detail : ruleDetailsList) {
                if (detail.getTotalCnt() == 0) {
                    detail.setGoodPercent(BigDecimal.ZERO);
                    detail.setOverduePercent(BigDecimal.ZERO);
                } else {
                    if (detail.getGoodCnt() == 0) {
                        detail.setGoodPercent(BigDecimal.ZERO);
                    } else {
                        detail.setGoodPercent(new BigDecimal(detail.getGoodCnt() / detail.getTotalCnt()));
                    }

                    if (detail.getOverdueCnt() == 0) {
                        detail.setOverduePercent(BigDecimal.ZERO);
                    } else {
                        detail.setOverduePercent(new BigDecimal(detail.getOverdueCnt() / detail.getTotalCnt()));
                    }
                }
            }
            robotRuleDetailDao.updateBatchById(ruleDetailsList);
        }
    }

    /**
     * 设置所有detail的total，good，bad的数量
     *
     * @param list
     * @param ruleList
     * @param ruleDetailsList
     */
    private void setRobotLeanData(List<Map<String, Object>> list, List<RobotRule> ruleList, List<RobotRuleDetail> ruleDetailsList) {
        if (null != ruleList && ruleList.size() > 0 && null != ruleDetailsList && ruleDetailsList.size() > 0) {

            for (Map<String, Object> map : list) {
                try {

                    String nid = (String) map.get("orderNo");// 订单号
                    int status = Integer.valueOf(String.valueOf(map.get("state"))); //0坏户，2好户

                    // 查询请求参数
                    DecisionReqLog decisionReqLog = decisionReqLogDao.getbyNid(nid);
                    if (null != decisionReqLog) {

                        // 入参
                        DecisionHandleRequest request = JSONObject.parseObject(decisionReqLog.getReqData(), DecisionHandleRequest.class);

                        Map<Long, RobotRuleDetail> detailMap = new HashMap<>();

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

                            Integer count = (Integer) methodObj.invoke(handlerObj, request);

                            // 3.查询方法返回值对应的规则明细
                            this.setRobotDetailData(ruleDetailsList, detailMap, robotRule.getId(), count, status);
                        }

                    }
                } catch (Exception e) {
                    log.error("训练模型异常", e);
                    continue;
                }
            }
        }
    }

    /**
     * 设置detail
     *
     * @param ruleDetailsList
     * @param ruldId          规则id
     * @param count           个数
     * @param status          0：坏户，2好户
     * @param detailMap
     * @return
     */
    private void setRobotDetailData(List<RobotRuleDetail> ruleDetailsList, Map<Long, RobotRuleDetail> detailMap, Long ruldId, Integer count, int status) {
        if (null != ruleDetailsList && ruleDetailsList.size() > 0) {

            for (RobotRuleDetail robotRuleDetail : ruleDetailsList) {
                if (robotRuleDetail.getRuleId().equals(ruldId)
                        && robotRuleDetail.getMinScope().compareTo(new BigDecimal(count))<=0
                        && robotRuleDetail.getMaxScope().compareTo(new BigDecimal(count))>0
                        && robotRuleDetail.getEnabled() == 1) {


                    // 计算好户坏户个数
                    robotRuleDetail.setTotalCnt(robotRuleDetail.getTotalCnt() + 1);
                    if (DecisionHandleRequest.USER_BAD == status) {
                        robotRuleDetail.setOverdueCnt(robotRuleDetail.getOverdueCnt() + 1);
                    }
                    if (DecisionHandleRequest.USER_GOOD == status) {
                        robotRuleDetail.setGoodCnt(robotRuleDetail.getGoodCnt() + 1);
                    }
                    break;
                }
            }
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
}
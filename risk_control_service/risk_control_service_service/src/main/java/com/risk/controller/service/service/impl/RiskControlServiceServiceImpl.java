package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.constans.ERROR;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.AdmissionResultDao;
import com.risk.controller.service.dao.AdmissionResultDetailDao;
import com.risk.controller.service.dao.DecisionReqLogDao;
import com.risk.controller.service.dao.DecisionRobotNoticeDao;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.entity.AdmissionResult;
import com.risk.controller.service.entity.AdmissionResultDetail;
import com.risk.controller.service.entity.DecisionReqLog;
import com.risk.controller.service.entity.DecisionResultLabel;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.DecisionRobotService;
import com.risk.controller.service.service.DecisionRuleService;
import com.risk.controller.service.service.RiskControlServiceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 风控决策服务的业务层实现
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Service
@Slf4j
public class RiskControlServiceServiceImpl implements RiskControlServiceService {

    @Autowired
    private LocalCache localCache;
    @Autowired
    private AdmissionResultDao admissionResultDao;
    @Autowired
    private DecisionRuleService decisionRuleService;
    @Autowired
    private DecisionRobotService robotService;
    @Autowired
    private DecisionRobotNoticeDao decisionRobotNoticeDao;
    @Autowired
    private AsyncTaskServiceImpl asyncTaskService;
    @Autowired
    private DecisionReqLogDao decisionReqLogDao;
    @Autowired
    private AdmissionResultDetailDao admissionResultDetailDao;

    private final static Integer ONE = 1;

    @Override
    @Scheduled(cron = "0 0/3 * * * ? ")
    public void reRunDecision() {
        log.warn("定时任务：重跑风控");
        List<String> result = admissionResultDao.queryNeedReRun();
        // 2、重跑
        if (null != result && result.size() > 0) {
            for (String nid : result) {
                if (StringUtils.isBlank(nid)) {
                    return;
                }
                try {
                    DecisionReqLog log = decisionReqLogDao.getbyNid(nid);
                    if (null == log) {
                        return;
                    }
                    DecisionHandleRequest request = JSONObject.parseObject(log.getReqData(), DecisionHandleRequest.class);
                    if (null != request) {
                        this.decisionHandle(request);
                    }
                } catch (Exception e) {
                    log.error("风控重跑异常：订单号：{}，e", nid, e);
                }
            }
        }
    }

    @Override
    public ResponseEntity getDecisionDetail(String nid) {
        if (StringUtils.isBlank(nid)) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, "订单号不能为空");
        }
        List<Map<String, Object>> list = admissionResultDetailDao.getDecisionDetail(nid);
        return new ResponseEntity(ResponseEntity.STATUS_OK, list);
    }

    @Override
    public ResponseEntity decisionHandle(DecisionHandleRequest request) {

        // 保存请求参数到数据库
        this.saveDecisionRequest(request);

        // 接收到数据存入数据库中
        AdmissionResultDTO ret = new AdmissionResultDTO(); // 决策最终结果
        ret.setResult(AdmissionResultDTO.RESULT_APPROVED); // 默认结果状态
        ret.setRobotAction(AdmissionResultDTO.ROBOT_ACTION_SCORE);
        ret.setRejectReason(new LinkedHashSet<>());

        // 查询订单是否需要重跑 设置AdmissionResult、ResultDetailMap
        decisionRuleService.getAdmissionSuspendContext(request);
        AdmissionResult admissionResult = this.getAdmissionResult(request, ret);
        Long labelGroupId = admissionResult.getLabelGroupId();

        if (null == admissionResult || null == admissionResult.getLabelGroupId() || admissionResult.getLabelGroupId() < 1) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, ERROR.ErrorMsg.NOT_GROUP, null);
        }
        // 异步处理决策
        asyncTaskService.asyncHandler(request, admissionResult, labelGroupId, ret);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    /**
     * 保存请求参数
     *
     * @param request
     */
    private void saveDecisionRequest(DecisionHandleRequest request) {
        try {
            DecisionReqLog log = new DecisionReqLog(request.getNid(), JSONObject.toJSONString(request));
            decisionReqLogDao.saveOrUpdate(log);
        } catch (Exception e) {
            log.error("保存请求数据异常，request：{}", JSONObject.toJSONString(request), e);
        }
    }

    /**
     * 设置标签id和adminssion
     *
     * @param request
     * @param ret
     * @return
     */
    private AdmissionResult getAdmissionResult(DecisionHandleRequest request, AdmissionResultDTO ret) {
        AdmissionResult admissionResult = null;
        Long labelGroupId = null;
        if (request.getAdmissionResult() == null) {
            admissionResult = new AdmissionResult();

            admissionResult.setNid(request.getNid());
            admissionResult.setResult(0);
            admissionResult.setTimeCost(0L);
            admissionResult.setLabelTimeCost(0L);
            admissionResult.setFailFast(request.getFailFast());
            admissionResult.setStopStage(0);
            admissionResult.setSuspendCnt(0);
            admissionResult.setSuspendStage(0);
            admissionResult.setSuspendTime(System.currentTimeMillis());
            admissionResult.setRobotAction(AdmissionResultDTO.ROBOT_ACTION_SCORE); // 模型动作评分
            admissionResult.setAddTime(System.currentTimeMillis());
            admissionResult.setUpdateTime(System.currentTimeMillis());
            this.admissionResultDao.insertSelective(admissionResult); //生成id
        } else {
            admissionResult = request.getAdmissionResult();
        }


        if (admissionResult.getLabelGroupId() == null || 0L == admissionResult.getLabelGroupId()) {
            // 定义存储标签Id的集合
            Set<Long> labelIdSet = new HashSet<>();

            // 打标签开始时间
            long labelTimeBegin = System.currentTimeMillis();
            // 获取决策标签decisionLabel信息
            Set<DecisionResultLabel> labelSet = this.decisionRuleService.getDecisionLabel(request);
            // 设置打标签时长
            admissionResult.setLabelTimeCost(System.currentTimeMillis() - labelTimeBegin);

            if (null != labelSet && !labelSet.isEmpty()) {
                labelSet.stream().forEach(o -> labelIdSet.add(o.getLabelId()));
                // 根据labelIds获取组Id
                labelGroupId = this.decisionRuleService.getDecisionLabelGroup(labelIdSet);
                ret.setLabels(labelSet);
            }
        } else {
            labelGroupId = admissionResult.getLabelGroupId();
        }
        admissionResult.setLabelGroupId(labelGroupId);
        ret.setId(admissionResult.getId());
        ret.setLabelGroupId(labelGroupId);
        return admissionResult;
    }
}

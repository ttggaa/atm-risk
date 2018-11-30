package com.risk.controller.service.service.impl;

import com.risk.controller.service.dao.*;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.enums.DecisionLabelCode;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.DecisionRuleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 决策规则业务层实现类
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Service
@Slf4j
public class DecisionRuleServiceImpl implements DecisionRuleService {

    @Autowired
    private DecisionLabelDao decisionLabelDao;

    @Autowired
    private DecisionLabelGroupMappingDao decisionLabelGroupMappingDao;

    @Autowired
    private DecisionLabelGroupDao decisionLabelGroupDao;

    @Autowired
    private AdmissionRuleDao admissionRuleDao;

    @Autowired
    private AdmissionResultDetailDao admissionResultDetailDao;

    @Autowired
    private AdmissionResultDao admissionResultDao;

    @Override
    public DecisionLabelGroup getLabelGroupById(Long labelGroupId) {
        return decisionLabelGroupDao.selectByPrimaryKey(labelGroupId);
    }

    @Override
    public List<AdmissionRule> getAdmissionRule(String merchantCode, Long labelGroupId, int stage) {
        List<AdmissionRule> ret = admissionRuleDao.getEnabledRuleByGroup(merchantCode, labelGroupId, stage);
        return ret;
    }

    @Override
    public void saveAdmissionResult(Long admissionResultId, List<AdmissionResultDetail> resultDetailList) {
        if (log.isDebugEnabled()) {
            log.debug("enter method, admissionResultId:{}, stage:{}, resultDetailList:{}", admissionResultId, resultDetailList);
        }

        if (null == resultDetailList) {
            return;
        }

        for (AdmissionResultDetail item : resultDetailList) {
            if (item.getId() == null) {
                item.setResultId(admissionResultId);
                item.setAddTime(System.currentTimeMillis());
                item.setUpdateTime(System.currentTimeMillis());
                this.admissionResultDetailDao.insertSelective(item);
            } else {
                item.setUpdateTime(System.currentTimeMillis());
                this.admissionResultDetailDao.updateByPrimaryKeySelective(item);
            }
        }
    }

    @Override
    public void getAdmissionSuspendContext(DecisionHandleRequest request) {
        if (null != request && StringUtils.isNotBlank(request.getNid())) {
            AdmissionResult admissionResult = this.admissionResultDao.getLastOneByNid(request.getNid(), request.getMerchantCode());
            if (null != admissionResult
                    && (AdmissionResultDTO.RESULT_SUSPEND == admissionResult.getResult()
                    || AdmissionResultDTO.RESULT_EXCEPTIONAL == admissionResult.getResult()
                    || 0 == admissionResult.getResult())) {

                request.setAdmissionResult(admissionResult);
                List<AdmissionResultDetail> detailList = getAdmissionResultDetail(admissionResult.getId());
                if (null != detailList) {
                    Map<Long, AdmissionResultDetail> detailMap = new HashMap<>();
                    for (AdmissionResultDetail d : detailList) {
                        detailMap.put(d.getRuleId(), d);
                    }
                    request.setResultDetailMap(detailMap);
                }
            }
        }
    }

    private List<AdmissionResultDetail> getAdmissionResultDetail(Long resultId) {
        if (log.isDebugEnabled()) {
            log.debug("enter method, resultId:{}", resultId);
        }
        List<AdmissionResultDetail> ret = null;
        if (null != resultId) {
            AdmissionResultDetail cond = new AdmissionResultDetail();
            cond.setResultId(resultId);
            ret = admissionResultDetailDao.getByResultId(cond);
        }
        log.debug("exit method, return:{}", ret);
        return ret;
    }

    /**
     * 根据用户手机号和手机号来判断是否新老用户 获取决策标签
     *
     * @param userName
     * @return
     */
    private Set<DecisionResultLabel> getDecisionLabel(String userName, Long merchantId) {
        if (log.isDebugEnabled()) {
            log.debug("DecisionRuleServiceImpl getDecisionLabel, userName:{}, merchantId:{}", userName, merchantId);
        }
        Set<DecisionResultLabel> ret = new HashSet();
        // 状态，0：待审核；1：审核成功；2：审核失败；3：协议确认； 4：财务建账
        int all = 0;
        int ok = 0;
        long timeBegin = System.currentTimeMillis();
        // TODO 调用sass系统获取指定用户的订单信息
        //List<Map<String, Number>> times = this.borrowService.getBorrowTimes(userId, null);
        List<Map<String, Number>> times = null;
        long timeCost = System.currentTimeMillis() - timeBegin;
        if (null != times) {
            for (Map<String, Number> o : times) {
                int status = o.get("status").intValue();
                int cnt = o.get("cnt").intValue();
                all += cnt;
                if (1 == status) {
                    ok += cnt;
                }
            }
        }
        // 根据返回的结果获取对应的标签
        DecisionLabel label = null;
        if (ok > 0) { // 旧户
            label = this.getLabel(merchantId, DecisionLabelCode.OLDLABEL.toString());
        } else {
            if (all > 1) { // 次新户
                label = this.getLabel(merchantId, DecisionLabelCode.SNEWLABEL.toString());
            } else { // 新户
                label = this.getLabel(merchantId, DecisionLabelCode.NEWLABEL.toString());
            }
        }
        DecisionResultLabel decisionResultLabel = DecisionResultLabel.fromDecisionLabel(label);
        if (decisionResultLabel != null) {
            decisionResultLabel.setTimeCost(timeCost);
            ret.add(decisionResultLabel);
        }
        log.debug("exit method, return:{}", ret);
        return ret;
    }

    /**
     * 获取老用户的标签
     *
     * @return
     */
    private DecisionLabel getLabel(Long merchantId, String code) {
        DecisionLabel findDecisionLabel = new DecisionLabel();
        findDecisionLabel.setMerchantId(merchantId);
        findDecisionLabel.setCode(code);
        DecisionLabel ret = decisionLabelDao.getByMerchantIdAndCode(findDecisionLabel);
        if (log.isDebugEnabled()) {
            log.debug("exit method, return: {}", ret);
        }
        return ret;
    }
}

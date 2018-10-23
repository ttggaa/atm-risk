package com.risk.controller.service.service;

import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionSuspendContextDTO;
import com.risk.controller.service.entity.AdmissionResultDetail;
import com.risk.controller.service.entity.AdmissionRule;
import com.risk.controller.service.entity.DecisionLabelGroup;
import com.risk.controller.service.entity.DecisionResultLabel;
import com.risk.controller.service.request.DecisionHandleRequest;

import java.util.List;
import java.util.Set;

/**
 * 决策规则业务层接口
 *
 * @Author ZT
 * @create 2018-08-27
 */
public interface DecisionRuleService {

    /**
     * 获取用户对应的决策标签
     * @param request
     * @return
     */
    Set<DecisionResultLabel> getDecisionLabel(DecisionHandleRequest request);

    /**
     * 根据标签Id集 获取对应的组id
     * @param labelIdSet
     * @return
     */
    Long getDecisionLabelGroup(Set<Long> labelIdSet);

    /**
     * 根据组id获取标签组信息
     * @param labelGroupId
     * @return
     */
    DecisionLabelGroup getLabelGroupById(Long labelGroupId);

    /**
     * 根据组id和步骤获取执行规则集
     * @param labelGroupId
     * @param stage
     * @return
     */
    List<AdmissionRule> getAdmissionRule(Long labelGroupId, int stage);

    /**
     * 保存决策结果
     * @param admissionResultId
     * @param resultDetailList
     */
    void saveAdmissionResult(Long admissionResultId, List<AdmissionResultDetail> resultDetailList);

    /**
     * 根据订单号查询该订单的风控决策结果
     * @param request
     * @return
     */
    void getAdmissionSuspendContext(DecisionHandleRequest request);
}

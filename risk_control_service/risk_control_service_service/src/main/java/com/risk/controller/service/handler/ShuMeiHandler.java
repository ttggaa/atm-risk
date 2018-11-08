package com.risk.controller.service.handler;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.impl.LocalCache;
import com.risk.controller.service.util.AdmissionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 树美接口
 */
@Slf4j
@Service
public class ShuMeiHandler implements AdmissionHandler {

    @Resource
    private LocalCache localCache;
    @Resource
    private MongoHandler mongoHandler;

    /**
     * 1045 数美多头借贷
     *
     * @return
     */
    public AdmissionResultDTO verifyShumeiData(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();

        if (rule.getSetting() == null
                || rule.getSetting() == null
                || !rule.getSetting().containsKey("minCount")) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("未配置规则");
            return result;
        }

        JSONObject rs = this.mongoHandler.getShumeiMultipoint(request);
        if (null == rs || null == rs.get("detail")) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("detail");
            return result;
        }

        JSONObject detail = rs.getJSONObject("detail");
        if (null == detail || null == detail.get("itfin_loan_applications_7d")) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData(null);
        }

        int applyCount = detail.getInteger("itfin_loan_applications_7d");
        int minCount = Integer.valueOf(rule.getSetting().get("minCount"));
        request.getRobotRequestDTO().setUserShumeiCount(applyCount);

        result.setData(applyCount);
        if (applyCount >= minCount) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        }
    }

    /**
     * 1051 树美在多个不同网贷平台被拒绝
     *
     * @return
     */
    public AdmissionResultDTO verifyShumeiReject(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();

        if (rule.getSetting() == null
                || rule.getSetting() == null
                || !rule.getSetting().containsKey("minCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("未配置规则");
            return result;
        }

        JSONObject rs = this.mongoHandler.getShumeiMultipoint(request);
        if (null == rs || null == rs.get("detail")) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("detail");
            return result;
        }

        JSONObject detail = rs.getJSONObject("detail");
        if (null == detail || null == detail.get("itfin_loan_refuses")) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("itfin_loan_refuses");
            result.setData(null);
        }

        int rejectCount = detail.getInteger("itfin_loan_refuses");
        int minCount = Integer.valueOf(rule.getSetting().get("minCount"));
        result.setData(rejectCount);

        if (rejectCount >= minCount) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        }
    }


    /**
     * 树美在多少个平台提出过申请
     *
     * @return
     */
    public AdmissionResultDTO verifyShumeApply(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();

        if (rule.getSetting() == null
                || rule.getSetting() == null
                || !rule.getSetting().containsKey("minCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("未配置规则");
            return result;
        }
        JSONObject rs = this.mongoHandler.getShumeiMultipoint(request);

        if (null == rs || null == rs.get("detail")) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("detail");
            return result;
        }

        JSONObject detail = rs.getJSONObject("detail");
        if (null == detail || null == detail.get("itfin_loan_applications")) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("itfin_loan_applications");
            result.setData(null);
        }

        int applications = detail.getInteger("itfin_loan_applications");
        int minCount = Integer.valueOf(rule.getSetting().get("minCount"));

        result.setData(applications);
        if (applications >= minCount) {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        }
    }

}

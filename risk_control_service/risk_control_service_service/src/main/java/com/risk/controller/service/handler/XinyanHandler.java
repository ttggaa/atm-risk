package com.risk.controller.service.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.XinyanService;
import com.risk.controller.service.util.AdmissionHandler;
import com.risk.controller.service.utils.xinyan.dto.XinyanRadarParamDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 新颜
 */
@Slf4j
@Service
public class XinyanHandler implements AdmissionHandler {
    @Resource
    private XinyanService xinyanService;

    /**
     * 1042
     * 新颜决策
     **/
    public AdmissionResultDTO verifyXinyanData(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        try {

            if (rule.getSetting() == null
                    || rule.getSetting() == null
                    || !rule.getSetting().containsKey("minCount")) {

                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("未配置规则");
                return result;
            }
            String minCount = rule.getSetting().get("minCount");

            // 新颜-行为雷达
            XinyanRadarParamDTO param = new XinyanRadarParamDTO();
            param.setIdName(request.getName());
            param.setIdNo(request.getCardId());
            ResponseEntity rs = this.xinyanService.getRadarBehavior(param, true);

            JSONObject xinyanBehaviorJson = (JSONObject) JSON.toJSON(rs);
            if (null == xinyanBehaviorJson || "0".equals(xinyanBehaviorJson.getString("code"))) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("code");
                return result;
            }

            JSONObject data = xinyanBehaviorJson.getJSONObject("data");
            if (null == data || null == data.get("result_detail")) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("result_detail");
                return result;
            }

            JSONObject resultDetail = data.getJSONObject("result_detail");
            if (null == resultDetail.get("loans_overdue_count")) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("loans_overdue_count");
                return result;
            }

            Integer loansOverdueCount = resultDetail.getInteger("loans_overdue_count");
            if (loansOverdueCount <= Integer.valueOf(minCount)) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                result.setData(loansOverdueCount);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData(loansOverdueCount);
                return result;
            }
        } catch (Exception e) {
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("程序异常");
            return result;
        }
    }


    /**
     * 新颜申请雷达
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyXinyanApplyData(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        try {

            if (rule.getSetting() == null
                    || rule.getSetting() == null
                    || !rule.getSetting().containsKey("minScore")) {

                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("未配置规则");
                return result;
            }

            XinyanRadarParamDTO param = new XinyanRadarParamDTO();
            param.setIdName(request.getName());
            param.setIdNo(request.getCardId());
            ResponseEntity rs = this.xinyanService.getRadarApply(param, true);

            /**
             * 0：查询成功
             * 1：查询未命中
             * 9：其他异常
             **/
            JSONObject apply = (JSONObject) JSON.toJSON(rs);
            if (null == apply || "0".equals(apply.getString("code"))) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("status");
                return result;
            }

            JSONObject data = apply.getJSONObject("data");
            if (null == data || null == data.get("result_detail")) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("result_detail");
                return result;
            }

            JSONObject resultDetail = data.getJSONObject("result_detail");
            if (null == resultDetail.get("apply_score")) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("apply_score");
                return result;
            }

            int minCount = Integer.valueOf(rule.getSetting().get("minScore"));
            int applyScore = resultDetail.getInteger("apply_score");
            result.setData(applyScore);

            if (applyScore >= minCount) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }

        } catch (Exception e) {
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("程序异常");
            return result;
        }
    }


    /**
     * 新颜行为雷达
     *
     * @param request
     * @param rule
     * @return
     */

    public AdmissionResultDTO verifyXinyanBehaviorData(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        try {

            if (rule.getSetting() == null
                    || rule.getSetting() == null
                    || !rule.getSetting().containsKey("minScore")) {

                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("未配置规则");
                return result;
            }


            // 新颜-行为雷达
            XinyanRadarParamDTO param = new XinyanRadarParamDTO();
            param.setIdName(request.getName());
            param.setIdNo(request.getCardId());
            ResponseEntity rs = this.xinyanService.getRadarBehavior(param, true);

            JSONObject xinyanBehaviorJson = (JSONObject) JSON.toJSON(rs);
            if (null == xinyanBehaviorJson || "0".equals(xinyanBehaviorJson.getString("code"))) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("code");
                return result;
            }

            JSONObject data = xinyanBehaviorJson.getJSONObject("data");
            if (null == data || null == data.get("result_detail")) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("result_detail");
                return result;
            }

            JSONObject resultDetail = data.getJSONObject("result_detail");
            if (null == resultDetail.get("loans_score")) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("loans_score");
                return result;
            }


            int minCount = Integer.valueOf(rule.getSetting().get("minScore"));
            int loansScore = resultDetail.getInteger("loans_score");
            result.setData(loansScore);

            if (loansScore >= minCount) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }

        } catch (Exception e) {
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("程序异常");
            return result;
        }
    }

}

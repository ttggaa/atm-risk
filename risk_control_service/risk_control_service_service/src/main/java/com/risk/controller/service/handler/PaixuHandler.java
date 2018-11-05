package com.risk.controller.service.handler;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.PaixuService;
import com.risk.controller.service.util.AdmissionHandler;
import com.risk.controller.service.utils.CSVUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Random;

/**
 * 排序
 */
@Slf4j
@Service
public class PaixuHandler implements AdmissionHandler {
    @Resource
    private PaixuService paixuService;
    @Resource
    private RobotHandler robotHandler;

    /**
     * 1049
     * {"checkScore":"450","passScore":"580","excludeScore":"-777,0","randomNum":"10","passPercent":"0.34","passCount":"0"}
     * checkScore：人工审核分数最低值
     * passScore：直接通过最低分值
     * excludeScore:排除分数（支持多个，用逗号隔开）
     **/
    public AdmissionResultDTO verifyPaixuDecision(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        if ((new Integer(2)).equals(request.getRobotRequestDTO().getModelNum())) {
            AdmissionResultDTO result = new AdmissionResultDTO();
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData(request.getRobotRequestDTO().getModelNum());
            return result;
        } else {
            return this.verifyPaixuDecisionV2(request, rule);
        }
    }

    /**
     * 排序模型
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyPaixuDecisionV2(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        try {

            if (rule == null
                    || rule.getSetting() == null
                    || !rule.getSetting().containsKey("checkScore")
                    || !rule.getSetting().containsKey("passScore")) {

                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                result.setData("未配置规则");
                return result;
            }
            Double ruleCheckScore = Double.valueOf(rule.getSetting().get("checkScore"));
            Double rulePassScore = Double.valueOf(rule.getSetting().get("passScore"));
            String ruleExcludeScore = rule.getSetting().get("excludeScore");
            String ruleExScores[] = StringUtils.isBlank(ruleExcludeScore) ? new String[]{} : ruleExcludeScore.split(",");


            ResponseEntity rs = paixuService.requestPaixu(request);

            if (null != rs && "1".equals(rs.getStatus()) && null != rs.getData()) {
                JSONObject paixuJson = JSONObject.parseObject(JSONObject.toJSONString(rs.getData()));

                if (null != paixuJson && "000000".equals(paixuJson.getString("rspCode"))) {

                    String robotscoreStr = paixuJson.getString("score");
                    Double robotscore = StringUtils.isBlank(robotscoreStr) ? 0d : Double.valueOf(robotscoreStr);

                    result.setData(robotscore);

                    if (robotscore >= rulePassScore) {
                        result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                        return result;
                    }
                    if (robotscore >= ruleCheckScore) {
                        result.setResult(AdmissionResultDTO.RESULT_MANUAL);
                        return result;
                    }

                    for (String ruleExScore : ruleExScores) {
                        if (Double.valueOf(ruleExScore).equals(robotscore)) {
                            result.setResult(AdmissionResultDTO.RESULT_MANUAL);
                            return result;
                        }
                    }

                    // 如果排序分数小于规则分数，拒绝
                    result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                    result.setData(robotscore);
                    return result;
                } else {
                    result.setData("排序返回异常");
                    result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
                    return result;
                }
            } else {
                result.setData("模型异常");
                result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
                return result;
            }
        } catch (Exception e) {
            log.error("排序模型程序异常，request:{},error:", JSONObject.toJSONString(request), e);
            result.setData("程序异常");
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }
}

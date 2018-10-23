package com.risk.controller.service.web;

import com.risk.controller.service.common.constans.ERROR;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.DecisionService;
import com.risk.controller.service.service.RiskControlServiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 风控决策服务的控制层
 *
 * @Author ZT
 * @create 2018-08-27
 */
@RestController
@RequestMapping("api/risk")
@Slf4j
public class RiskControlServiceController {

    @Autowired
    private RiskControlServiceService riskControlServiceService;

    @Autowired
    private DecisionService decisionService;

    /**
     * 决策处理入口
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/decision", method = RequestMethod.POST)
    public ResponseEntity decision(@Validated DecisionHandleRequest request) {
        try {
            return riskControlServiceService.decisionHandle(request);
        } catch (Exception e) {
            log.error("decision 执行异常，result：{}，error", request, e);
        }
        return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "程序异常", null);
    }

    /**
     * 查询决策通过或者拒绝具体原因
     * @param nid
     * @return
     */
    @RequestMapping(value = "/decisionDetail", method = RequestMethod.GET)
    public ResponseEntity decisionDetail(String nid) {
        return riskControlServiceService.getDecisionDetail(nid);
    }


    /**
     * 重跑风控决策（定时任务每3分钟跑一次）
     * @return
     */
    @RequestMapping(value = "/reRunDecision", method = RequestMethod.GET)
    public ResponseEntity reRunDecision() {
        riskControlServiceService.reRunDecision();
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }


    /**
     * 重复推送失败的数据给业务系统
     * 定时任务每10分钟跑一次
     *
     * @param nid       订单号，可以不传（查询最近30条）
     * @param noticeNum 重复推送次数，不传默认5
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "pushRiskResult", method = RequestMethod.GET)
    public ResponseEntity pushRiskResult(String nid, Integer noticeNum) {
        return decisionService.pushRiskResult(nid, noticeNum);
    }
}

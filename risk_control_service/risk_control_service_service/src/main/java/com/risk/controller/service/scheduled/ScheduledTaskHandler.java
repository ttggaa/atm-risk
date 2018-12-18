package com.risk.controller.service.scheduled;

import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.service.DecisionService;
import com.risk.controller.service.service.RiskControlServiceService;
import com.risk.controller.service.service.impl.LocalCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 *
 * @Author: Tonny
 * @CreateDate: 18/11/30 下午 03:57
 * @Version: 1.0
 */
@Component
@Slf4j
public class ScheduledTaskHandler {
    @Autowired
    private DecisionService decisionService;
    @Autowired
    private RiskControlServiceService riskControlServiceService;
    @Autowired
    private LocalCache localCache;

    /**
     * 重新推送风控结果
     *
     * @return
     */
    @Scheduled(cron = "0 0/10 * * * ? ")
    public void pushRiskResult() {
        log.warn("定时任务开始：重新推送风控结果");
        String num = localCache.getLocalCache(CacheCfgType.SYSTEMCFG, "atm.push.result.num");
        num = StringUtils.isBlank(num) ? "5" : num;
        this.decisionService.pushRiskResult(null, Integer.valueOf(num));
    }

    /**
     * 重跑风控
     */
    @Scheduled(cron = "0 0/3 * * * ? ")
    public void reRunDecision() {
        log.warn("定时任务：重跑风控");
        riskControlServiceService.reRunDecision();
    }
}

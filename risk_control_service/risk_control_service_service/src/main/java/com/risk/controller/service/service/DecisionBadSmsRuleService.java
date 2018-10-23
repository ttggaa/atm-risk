package com.risk.controller.service.service;

import com.risk.controller.service.dao.DecisionBadSmsRuleDao;
import com.risk.controller.service.entity.DecisionBadSmsRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


@Slf4j
@Service
public class DecisionBadSmsRuleService {

    @Resource
    private DecisionBadSmsRuleDao decisionBadSmsRuleDao;

    public List<DecisionBadSmsRule> getEnabled() {
        return decisionBadSmsRuleDao.getAll();
    }
}

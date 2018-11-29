package com.risk.controller.service.web;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.dto.RobotResultRequest;
import com.risk.controller.service.handler.RobotHandler;
import com.risk.controller.service.handler.VerifyHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.ModelDataService;
import com.risk.controller.service.service.ModelService;
import com.risk.controller.service.service.OperatorService;
import com.risk.controller.service.service.impl.PaixuServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("model")
@Controller
@Slf4j
public class ModelController {

    @Autowired
    private ModelService modelService;
    @Autowired
    private RobotHandler robotHandler;
    @Autowired
    private OperatorService operatorService;
    @Autowired
    private ModelDataService modelDataService;

    /**
     * 计算用户运营商数据
     *
     * @param nid
     * @param type
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/verifyUserOperator", method = RequestMethod.GET)
    public ResponseEntity verifyUserOperator(String nid, Integer type) {
        modelService.verifyUserOperator(nid, type);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    /**
     * 模型训练
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/modelLearn", method = RequestMethod.GET)
    public ResponseEntity modelLearn(Long ruleId) {
        modelService.modelLearn(ruleId);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    @ResponseBody
    @RequestMapping(value = "/saveAllOperator", method = RequestMethod.GET)
    public ResponseEntity saveAllOperator(String nid) {
        try {
            operatorService.saveAllOperator(nid);
        } catch (Exception e) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL);
        }
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    @ResponseBody
    @RequestMapping(value = "/runModelBySql", method = RequestMethod.POST)
    public ResponseEntity runModelBySql(String sql, Integer source) {
        if (null == source || 0 >= source || 4 <= source) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, "请输入模式。" +
                    "source=1正式数据，查询生产模型区间；" +
                    "source=2训练数据，查询训练模型区间；" +
                    "source=3训练数据，查询生产模型区间");
        }
        robotHandler.runModelBySql(sql, source);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    @ResponseBody
    @RequestMapping(value = "/saveDataBySql", method = RequestMethod.POST)
    public ResponseEntity saveDataBySql(String sql) {
        modelDataService.saveDataBySql(sql);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }
}

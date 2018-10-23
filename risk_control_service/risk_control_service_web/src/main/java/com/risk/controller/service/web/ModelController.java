package com.risk.controller.service.web;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.dto.RobotResultRequest;
import com.risk.controller.service.handler.VerifyHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.ModelService;
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
    public ResponseEntity modelLearn() {
        modelService.modelLearn();
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    @ResponseBody
    @RequestMapping(value = "/saveAllOperator", method = RequestMethod.GET)
    public ResponseEntity saveAllOperator(String nid) {
        modelService.saveAllOperator(nid);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }
}
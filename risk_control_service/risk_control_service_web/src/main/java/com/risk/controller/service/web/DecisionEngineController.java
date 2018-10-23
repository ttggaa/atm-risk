package com.risk.controller.service.web;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dto.RobotResultRequest;
import com.risk.controller.service.service.DecisionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author ZT
 * @create 2018-08-27
 */
@RestController
@RequestMapping("api/1.0.0/decisionEngine")
@Slf4j
public class DecisionEngineController {

    @Autowired
    private DecisionService decisionService;

    @ResponseBody
    @RequestMapping(value = "/robotResultNotice",method = RequestMethod.POST)
    public ResponseEntity robotResultNotice(RobotResultRequest request){
        log.debug("通知结果：{}",request);
        decisionService.robotResultNotice(request,null,true);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }


    /**
     * test:
     * curl  -H'Content-uid:1' -H'Content-pwkey:1' -i http://localhost:8080/custinternal/api/1.0.0/machineStudy/getAllData -d 'nid=201705260000006164'
     * 为了打分跑的,模型线上打分数据接口
     *
     * @param nid
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "getAllData", method = RequestMethod.POST)
    public ResponseEntity getAllData(String nid) {
        ResponseEntity rs = decisionService.getAllDataByNid(nid);
        return rs;
    }
}

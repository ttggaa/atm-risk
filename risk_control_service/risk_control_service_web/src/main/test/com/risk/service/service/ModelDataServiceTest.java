package com.risk.service.service;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dao.DecisionReqLogDao;
import com.risk.controller.service.entity.DecisionReqLog;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.ModelDataService;
import com.risk.controller.service.service.ModelService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBootStart.class)
public class ModelDataServiceTest {

    @Resource
    private ModelDataService modelDataService;
    @Resource
    private DecisionReqLogDao decisionReqLogDao;
    @Resource
    private ModelService modelService;

    @Test
    public void saveData() throws Exception {
        String sql = "SELECT nid FROM risk_decision_req_log where nid in ('218090716140277998','218101817464814150','218101913572114374','31806021379960164','31806050084165325');";
        List<Map<String, Object>> list = this.modelService.runModelBySql(sql);
        for (Map<String, Object> map : list) {
            try {
                String nid = map.get("nid").toString();
                DecisionReqLog reqLog = decisionReqLogDao.getbyNid(nid);
                if (null != reqLog) {
                    String reqDate = reqLog.getReqData();
                    DecisionHandleRequest request = JSONObject.parseObject(reqDate, DecisionHandleRequest.class);
                    modelDataService.saveData(request);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Test
    public void saveData2() throws Exception {
        String nid = "31806021379960164";
        nid = "31806050084165325";
//        nid = "218101913572114374";
        DecisionReqLog reqLog = decisionReqLogDao.getbyNid(nid);
        if (null != reqLog) {
            String reqDate = reqLog.getReqData();
            DecisionHandleRequest request = JSONObject.parseObject(reqDate, DecisionHandleRequest.class);
            modelDataService.saveData(request);
        }
    }
}

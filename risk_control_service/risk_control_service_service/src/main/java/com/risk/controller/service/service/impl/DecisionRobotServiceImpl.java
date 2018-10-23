package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.dao.DecisionRobotResultDao;
import com.risk.controller.service.dao.DecisionRobotScoreResultDao;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.RobotResultRequest;
import com.risk.controller.service.dto.RobotScoreDTO;
import com.risk.controller.service.entity.DecisionRobotResult;
import com.risk.controller.service.entity.DecisionRobotScoreResult;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.DecisionRobotService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 机器人打分的业务层实现
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Service
@Slf4j
public class DecisionRobotServiceImpl implements DecisionRobotService {

    @Autowired
    private LocalCache localCache;

    @Autowired
    private DecisionRobotScoreResultDao decisionRobotScoreResultDao;

    @Autowired
    private DecisionRobotResultDao decisionRobotResultDao;

    private static final String ROBOT_REJECT_REASON_CODE = "R1812";

    @Override
    public RobotScoreDTO getRobotScore(DecisionHandleRequest request, boolean isSaveInput, boolean isSaveOut) {
        if (log.isDebugEnabled()) {
            log.debug("enter method, decisionHandleRequest:{}, isSaveInput:{}, isSaveOut:{}", request, isSaveInput, isSaveOut);
        }

        String nid = request.getNid();
        RobotScoreDTO ret = new RobotScoreDTO();
        ret.setNid(nid);

        //调用custinternal 获取json
        String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "robot.score.url");
        String uid = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.SYSTEMCFG, "rcs.uid");
        String pwkey = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.SYSTEMCFG, "rcs.pwkey");

        //超时处理
        Map<String,Object> config = new HashMap<String, Object>();
        config.put(HttpClientUtils.CONNECT_TIMEOUT, 30000);
        config.put(HttpClientUtils.SOCKET_TIMEOUT, getTimeout());
        HttpClientUtils.setConfig(config);
        Map<String, String> header = new HashMap<String, String>();
        header.put("Content-uid", uid);
        header.put("Content-pwkey", pwkey);
        HttpClientUtils.setHeader(header);

        Map<String, String> params = new HashMap<String, String>();
        params.put("nid", nid);
//        params.put("productId", String.valueOf(request.getProductId()));

        Long startTime = System.currentTimeMillis();
        String scoreString = "";
        try {
            scoreString = HttpClientUtils.doGet(url, params);
        } catch (Exception e) {
            log.error("robot score error:", url);
            log.error("robot score error, params:", params);

            ret.setStatus(2);//没有获取到json 数据
            ret.setStartTime(startTime);
            ret.setEndTime(System.currentTimeMillis());
            ret.setErrorMsg("查询评分数据异常:" + ExceptionUtils.getStackTrace(e));
        }

        if (StringUtils.isEmpty(scoreString)) {
            ret.setStatus(2);//没有获取到json 数据
            ret.setStartTime(startTime);
            ret.setEndTime(System.currentTimeMillis());
            ret.setErrorMsg("查询评分数据异常, upstream response empty");
        } else {
            try {
                JSONObject scoreObj = JSONObject.parseObject(scoreString);
                ret.setProbOf1(scoreObj.getBigDecimal("prob_of_1"));
                ret.setProbOf0(scoreObj.getBigDecimal("prob_of_0"));

                ret.setPreliminaryResult(scoreObj.getInteger("preliminary"));
                ret.setFinalResult(scoreObj.getInteger("final"));

                ret.setStatus(1);
                ret.setErrorMsg(scoreString);
            } catch (Exception ex){
                ret.setStatus(2);//没有获取到json 数据
                ret.setStartTime(startTime);
                ret.setEndTime(System.currentTimeMillis());
                ret.setErrorMsg(scoreString);
            }
        }

        if (null != ret){
            ret.setStartTime(startTime);
            ret.setEndTime(System.currentTimeMillis());

            if(isSaveOut) {
//                saveOut(ret);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("exit method, return:{}", ret);
        }

        return ret;
    }

    @Override
    public void saveOut(RobotScoreDTO ret) {
        DecisionRobotScoreResult miaobtRobotScoreResult = new DecisionRobotScoreResult();
        Long now = System.currentTimeMillis();
        miaobtRobotScoreResult.setAddTime(now);
        miaobtRobotScoreResult.setUpdateTime(now);
        //长度截取
        String errorMsg = ret.getErrorMsg();
        if (errorMsg.length() > 1024) {
            errorMsg = errorMsg.substring(0, 1024);
        }
        miaobtRobotScoreResult.setErrorMsg(errorMsg);
        miaobtRobotScoreResult.setProbOf0(ret.getProbOf0());
        miaobtRobotScoreResult.setProbOf1(ret.getProbOf1());
        miaobtRobotScoreResult.setNid(ret.getNid());
        miaobtRobotScoreResult.setStatus(ret.getStatus());
        miaobtRobotScoreResult.setStartTime(ret.getStartTime());
        miaobtRobotScoreResult.setEndTime(ret.getEndTime());
        decisionRobotScoreResultDao.insertSelective(miaobtRobotScoreResult);
    }

    @Override
    public DecisionRobotResult merge(AdmissionResultDTO admResult, RobotScoreDTO scoreDto, RobotResultRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("enter method, admResult:{}, scoreDto:{}, nid:{}", admResult, scoreDto, request.getNid());
        }

        DecisionRobotResult ret = new DecisionRobotResult();
        ret.setNid(request.getNid());
        ret.setAdmissionResult(admResult.getResult());
        ret.setResult(admResult.getResult());
        ret.setProbOf1(BigDecimal.ONE.negate());

        if(null == scoreDto || 1 != scoreDto.getStatus().intValue()){ // 维持原判
            log.warn("scoreDto is null or status is 0, keep original admission result, admResult:{}, scoreDto:{}", admResult, scoreDto);
            return ret;
        }
        ret.setProbOf1(scoreDto.getProbOf1());
        Integer ZERO = 0;
        if(AdmissionResultDTO.RESULT_FINAL_APPROVED == admResult.getResult()){ // 决策自动过
            if(ZERO.equals(scoreDto.getFinalResult())){ //通过
                admResult.setResult(AdmissionResultDTO.RESULT_APPROVED);
            } else { // 拒绝
                admResult.setResult(AdmissionResultDTO.RESULT_REJECTED);
                Set<String> reasonSet = admResult.getRejectReason();
                if (null == reasonSet) {
                    reasonSet = new HashSet<>();
                    admResult.setRejectReason(reasonSet);
                }
                reasonSet.add(ROBOT_REJECT_REASON_CODE);
            }
        } else {
            if(ZERO.equals(scoreDto.getPreliminaryResult())){ //通过
                admResult.setResult(AdmissionResultDTO.RESULT_APPROVED);
            } else {
                if(ZERO.equals(scoreDto.getFinalResult())){ //进人工
                    // nothing,
                } else { //拒绝
                    admResult.setResult(AdmissionResultDTO.RESULT_REJECTED);
                    Set<String> reasonSet = admResult.getRejectReason();
                    if (null == reasonSet) {
                        reasonSet = new HashSet<>();
                        admResult.setRejectReason(reasonSet);
                    }
                    reasonSet.add(ROBOT_REJECT_REASON_CODE);
                }
            }
        }

        ret.setResult(admResult.getResult());
        ret.setRuleId(0L);

         /** 机器人直接给结果
         RobotRuleDTO cond = new RobotRuleDTO();
         cond.setAdmissionResult(admResult.getResult());
         cond.setLabelGroupId(admResult.getLabelGroupId());
         cond.setSecuredType(borrow.getSecuredType());
         cond.setAmount(borrow.getAmount());
         cond.setBorrowDays(borrow.getBorrowDays());
         cond.setProbOf1(scoreDto.getProbOf1());

         List<DecisionRobotRule> ruleList = this.getMatchedRobotRule(cond);
         if(null == ruleList || ruleList.size() < 1){
         log.warn("robot rule list is empty, keep original admission result, admResult:{}, scoreDto:{}, rule query cond:{}", admResult, scoreDto, cond);
         return ret;
         }
         // 取第1条
         DecisionRobotRule rule = ruleList.get(0);
         admResult.setResult(rule.getResult());
         ret.setResult(admResult.getResult());

         if(StringUtils.isNotEmpty(rule.getRejectReasonCode())) {
         Set<String> reasonSet = admResult.getRejectReason();
         if (null == reasonSet) {
         reasonSet = new HashSet<>();
         admResult.setRejectReason(reasonSet);
         }
         reasonSet.add(rule.getRejectReasonCode());
         }
         ret.setRuleId(rule.getId());
         */
        if (log.isDebugEnabled()) {
            log.debug("exit method, return:{}", ret);
        }
        return ret;
    }

    @Override
    public void saveDecisionRobotResult(DecisionRobotResult robotResult) {
        if (log.isDebugEnabled()) {
            log.debug("enter method, robotResult:{}", robotResult);
        }
        if(null == robotResult){
            return;
        }
        robotResult.setAddTime(System.currentTimeMillis());
        robotResult.setUpdateTime(System.currentTimeMillis());
        decisionRobotResultDao.insertSelective(robotResult);
        if (log.isDebugEnabled()) {
            log.debug("exit method");
        }
    }

    /**
     * 单位： 毫秒
     * @return
     */
    private long getTimeout(){
        long ret = 180 * 1000;
        String timeoutConf = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.SYSTEMCFG, "robot.score.timeout");
        if(StringUtils.isNotEmpty(timeoutConf)){
            ret = Long.parseLong(timeoutConf) * 1000;
        }

        return ret;
    }
}

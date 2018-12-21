package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.controller.service.common.constans.ERROR;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.dto.*;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.mongo.dao.MongoDao;
import com.risk.controller.service.mongo.dao.MongoQuery;
import com.risk.controller.service.service.DecisionRobotService;
import com.risk.controller.service.service.DecisionService;
import com.risk.controller.service.service.MerchantInfoService;
import com.risk.controller.service.utils.DataBaseUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * @Author ZT
 * @create 2018-08-27
 */
@Service
@Slf4j
public class DecisionServiceImpl implements DecisionService {
    @Autowired
    private AdmissionResultDetailDao admissionResultDetailDao;
    @Autowired
    private LocalCache localCache;
    @Autowired
    private DecisionResultNoticeDao decisionResultNoticeDao;
    @Autowired
    private RejectReasonDao rejectReasonDao;
    @Autowired
    private MerchantInfoService merchantInfoService;

    /**
     * 通知业务系统风控结果
     *
     * @param nid
     * @param merchantCode
     * @param admResult
     * @return
     */
    public ResponseEntity noticeBorrowResultHandle(String nid, String merchantCode, AdmissionResultDTO admResult) {

        String enabled = merchantInfoService.getBymerchantCode(merchantCode);
        if (!"1".equals(enabled)) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, null, "商户号不存在");
        }

        String url = merchantInfoService.getMerchantUrl(merchantCode, "call_back_url");

        Map<String, Object> params = new HashMap<>();
        params.put("nid", nid);

        // 不等于通过，人工审核，直接拒绝
        if (admResult.getResult() != AdmissionResultDTO.RESULT_APPROVED && admResult.getResult() != AdmissionResultDTO.RESULT_MANUAL) {
            admResult.setResult(AdmissionResultDTO.RESULT_REJECTED);
        }

        String rejectReasons = admResult.getRejectReason() == null ? "" : StringUtils.join(admResult.getRejectReason(), ",");

        params.put("status", String.valueOf(admResult.getResult()));
        params.put("rejectReasons", rejectReasons);
        params.put("msg", String.valueOf(admResult.getResult()));

        String resultStr = null;
        String msg = null;
        try {
            resultStr = HttpClientUtils.doPost(url, JSONObject.toJSONString(params), "application/json");
            log.info("推送结果，订单号：{}，同步结果结果：{}", nid, resultStr);
        } catch (Throwable e) {
            log.error("推送结果异常，订单号：{}，同步结果结果：{}", nid, resultStr);
            if (null != e) {
                msg = e.getMessage();
            }
        }
        this.saveResult(nid, admResult, resultStr, msg, merchantCode);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    /**
     * 保存回调结果
     *
     * @param nid          订单号
     * @param admResult    决策结果
     * @param resultStr    业务系统回调结果
     * @param msg          异常信息
     * @param merchantCode 商户号
     */
    private void saveResult(String nid, AdmissionResultDTO admResult, String resultStr, String msg, String merchantCode) {
        try {
            msg = StringUtils.isNotBlank(resultStr) ? resultStr : msg;
            if (StringUtils.isNotBlank(msg) && msg.length() >= 4000) {
                msg = msg.substring(0, 3999);
            }
            JSONObject result = JSONObject.parseObject(resultStr);
            if (null != result && result.containsKey("code") && "0".equals(result.getString("code"))) {
                saveErrorNoticeSaas(merchantCode, nid, msg, admResult, 1);
            } else {
                saveErrorNoticeSaas(merchantCode, nid, msg, admResult, 2);
            }
        } catch (Exception e) {
            log.error("决策回调通知保存回调处理结果异常：nid:{},merchantCode:{},admResult:{},resultStr:{},msg:{}", nid, merchantCode, JSONObject.toJSONString(admResult), resultStr, msg, e);
        }
    }

    /**
     * 通知业务系统风控结果
     *
     * @param nid
     * @param noticeNum
     * @return
     */
    @Override
    public ResponseEntity pushRiskResult(String nid, Integer noticeNum) {
        if (noticeNum == null) {
            noticeNum = 5;
        }
        // 查询通知失败的记录
        List<DecisionResultNotice> noticeList = decisionResultNoticeDao.pushRiskResult(nid, noticeNum);
        if (null != noticeList && noticeList.size() > 0) {
            for (DecisionResultNotice notice : noticeList) {
                try {
                    AdmissionResultDTO admResult = new AdmissionResultDTO();
                    admResult.setResult(notice.getResult());
                    // 失败原因
                    if (StringUtils.isNotBlank(notice.getRejectReasons())) {
                        Set<String> rejectReason = new HashSet<>(Arrays.asList(notice.getRejectReasons().split(",")));
                        admResult.setRejectReason(rejectReason);
                    }
                    noticeBorrowResultHandle(notice.getNid(), notice.getMerchantCode(), admResult);
                } catch (Exception e) {
                    log.error("定时任务重新推送通知异常，nid:{},e:{}", nid, e);
                    continue;
                }
            }
        }
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    /**
     * 保存通知结果
     *
     * @param merchantCode
     * @param nid
     * @param admResult
     * @param status
     */
    public void saveErrorNoticeSaas(String merchantCode, String nid, String msg, AdmissionResultDTO admResult, Integer status) {
        DecisionResultNotice notice =new DecisionResultNotice();
        notice.setMerchantCode(merchantCode);
        notice.setNid(nid);
        notice = decisionResultNoticeDao.selectByCondition(notice);
        try {
            if (notice != null && notice.getStatus() != 1) {
                notice.setMerchantCode(merchantCode);
                notice.setStatus(status);
                notice.setResult(admResult.getResult());
                notice.setRejectReasons(admResult.getRejectReason() == null ? "" : StringUtils.join(admResult.getRejectReason(), ","));
                notice.setNoticeNum(notice.getNoticeNum() + 1);
                notice.setUpdateTime(System.currentTimeMillis());
                notice.setMsg(msg);
                decisionResultNoticeDao.updateByPrimaryKeySelective(notice);
            } else if (notice == null) {
                notice = new DecisionResultNotice();
                notice.setMerchantCode(merchantCode);
                notice.setStatus(status);
                notice.setResult(admResult.getResult());
                notice.setNoticeNum(1);
                notice.setNid(nid);
                notice.setRejectReasons(admResult.getRejectReason() == null ? "" : StringUtils.join(admResult.getRejectReason(), ","));
                notice.setAddTime(System.currentTimeMillis());
                notice.setMsg(msg);
                decisionResultNoticeDao.insert(notice);
            }
        } catch (Exception e) {
            log.error("通知更新异常：{}", e);
        }
    }
}

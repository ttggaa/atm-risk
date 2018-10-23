package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.AdmissionResultDao;
import com.risk.controller.service.dao.RiskXinyanLogDao;
import com.risk.controller.service.entity.RiskXinyanLog;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.mongo.dao.MongoCollections;
import com.risk.controller.service.mongo.dao.MongoDao;
import com.risk.controller.service.mongo.dao.MongoQuery;
import com.risk.controller.service.service.XinyanService;
import com.risk.controller.service.service.impl.LocalCache;
import com.risk.controller.service.utils.Base64;
import com.risk.controller.service.utils.xinyan.common.XinyanConstant;
import com.risk.controller.service.utils.xinyan.dto.XinyanRadarParamDTO;
import com.risk.controller.service.utils.xinyan.util.RsaCodingUtil;
import com.risk.controller.service.utils.xinyan.util.XinyanUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.data;

@Slf4j
@Service
public class XinyanServiceImpl implements XinyanService {

    @Autowired
    private LocalCache localCache;
    @Autowired
    private RiskXinyanLogDao riskXinyanLogDao;
    @Autowired
    private MongoDao mongoDao;

    @Override
    public ResponseEntity getRadarApply(XinyanRadarParamDTO param, boolean expire) {

        // 如果有历史数据，查询历史
        RiskXinyanLog riskXinyanLog = riskXinyanLogDao.getLastOne(param.getIdNo(), XinyanConstant.REQ_TYPE_APPLY);
        if (riskXinyanLog != null) {
            String hours = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.radar.hours");
            hours = StringUtils.isBlank(hours) ? "48" : hours;
            // 如果不校验过期，直接查询历史数据，或者在有效期内查询mongo数据
            if (!expire || (riskXinyanLog.getAddTime() + Integer.valueOf(hours) * 3600 * 1000) >= System.currentTimeMillis()) {
                MongoQuery query = new MongoQuery("id_no", param.getIdNo(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                JSONObject data = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_XINYAN_APPLY);
                if (null != data) {
                    data.remove("_id");
                    return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, data);
                }
            }
        }

        // 实时调用新颜接口数据
        String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.radar.apply.url");
        JSONObject respData = request(url, param, XinyanConstant.REQ_TYPE_APPLY);
        if (respData != null) {
            JSONObject data = respData.getJSONObject("data");
            if (null != data) {
                mongoDao.save(data, MongoCollections.DB_XINYAN_APPLY);
                return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, data);
            }
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, respData.getString("errorMsg"), null);
        }
        return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "查询数据失败", null);
    }

    @Override
    public ResponseEntity getRadarBehavior(XinyanRadarParamDTO param, boolean expire) {

        RiskXinyanLog riskXinyanLog = riskXinyanLogDao.getLastOne(param.getIdNo(), XinyanConstant.REQ_TYPE_BEHAVIOR);
        if (riskXinyanLog != null) {
            String hours = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.radar.hours");
            hours = StringUtils.isBlank(hours) ? "48" : hours;
            if (!expire || (riskXinyanLog.getAddTime() + Integer.valueOf(hours) * 3600 * 1000) >= System.currentTimeMillis()) {
                MongoQuery query = new MongoQuery("id_no", riskXinyanLog.getIdNo(), MongoQuery.MongoQueryBaseType.eq);
                List<MongoQuery> queries = new ArrayList<>();
                queries.add(query);
                JSONObject data = mongoDao.findOne(queries, JSONObject.class, MongoCollections.DB_XINYAN_BEHAVIOR);
                if (null != data) {
                    data.remove("_id");
                    return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, data);
                }
            }
        }
        String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.radar.behavior.url");
        JSONObject respData = request(url, param, XinyanConstant.REQ_TYPE_BEHAVIOR);
        if (respData != null) {
            JSONObject data = respData.getJSONObject("data");
            if (data != null) {
                mongoDao.save(data, MongoCollections.DB_XINYAN_BEHAVIOR);
                return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, data);
            }
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, respData.getString("errorMsg"), null);
        }
        return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "查询数据失败", null);
    }

    /**
     * 请求新颜接口数据
     *
     * @param url     请求url
     * @param param   清楚参数
     * @param reqType 请求类似
     * @return
     */
    private JSONObject request(String url, XinyanRadarParamDTO param, Integer reqType) {
        String memberId = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.memberId");
        String terminalId = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.terminalId");
        String industryType = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.radar.industryType");
        String versions = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.radar.versions");

        // 构建业务参数
        JSONObject bizParam = new JSONObject();
        bizParam.put("member_id", memberId);
        bizParam.put("terminal_id", terminalId);
        String transId = XinyanUtil.generateTransId();
        bizParam.put("trans_id", transId);
        bizParam.put("trade_date", DateUtils.formatDate(new Date(), "yyyyMMddHHmmss"));
        bizParam.put("industry_type", industryType);
        bizParam.put("id_no", param.getIdNo());
        bizParam.put("id_name", param.getIdName());
        bizParam.put("phone_no", param.getPhoneNo());
        bizParam.put("bankcard_no", param.getBankcardNo());
        bizParam.put("versions", versions);
        // 构建请求参数
        String bizParamStr = bizParam.toJSONString();
        String encodeData = null;
        try {
            encodeData = Base64.encode(bizParamStr.getBytes("UTF-8"));
        } catch (Exception ex) {
            throw new RuntimeException("新颜请求参数构建异常", ex);
        }
        String pfxPath = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.pfxPath");
        String pfxPwd = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "xinyan.pfxPwd");
        String dataContent = RsaCodingUtil.encryptByPriPfxFile(encodeData, pfxPath, pfxPwd);// 加密数据
        Map<String, String> requestParam = new HashMap<String, String>();
        requestParam.put("member_id", memberId);
        requestParam.put("terminal_id", terminalId);
        requestParam.put("data_type", "json");
        requestParam.put("data_content", dataContent);

        Long startTime = System.currentTimeMillis();
        String resp = null;
        try {
            resp = HttpClientUtils.doPost(url, requestParam); // 发起请求
        } catch (Throwable e) {
            log.error("【新颜请求异常】", e);
        }

        // 插入请求日志开始
        long endTime = System.currentTimeMillis();
        JSONObject respJson = JSON.parseObject(resp);
        RiskXinyanLog reqLog = new RiskXinyanLog();
        reqLog.setIdName(param.getIdName());
        reqLog.setIdNo(param.getIdNo());
        reqLog.setReqParam(bizParamStr);
        reqLog.setCode(reqType);
        reqLog.setTransId(transId);
        reqLog.setUrl(url);
        reqLog.setStatus(0);
        reqLog.setReqTime(startTime);

        reqLog.setReqCnt(endTime - startTime);
        reqLog.setAddTime(endTime);
        reqLog.setUpdateTime(endTime);
        try {
            if (StringUtils.isNotBlank(resp)) {
                reqLog.setRepParam(resp);
                reqLog.setStatus(respJson.getBooleanValue("success") ? 1 : 2);
                String errorCode = respJson.getString("errorCode");
                reqLog.setRepErrorcode(StringUtils.isBlank(errorCode) ? "" : errorCode);
                reqLog.setScore(0);
                if (null != respJson.get("data")
                        && null != respJson.getJSONObject("data").get("result_detail")) {

                    JSONObject detail = respJson.getJSONObject("data").getJSONObject("result_detail");
                    if (null != detail.get("loans_score")) {
                        reqLog.setScore(detail.getInteger("loans_score"));
                    }
                    if (null != detail.get("apply_score")) {
                        reqLog.setScore(detail.getInteger("apply_score"));
                    }
                }
                riskXinyanLogDao.insert(reqLog);
            } else {
                reqLog.setStatus(2);
                reqLog.setRepErrorcode("");
                riskXinyanLogDao.insert(reqLog);
            }
        } catch (Exception e) {
            log.error("插入请求日志表失败", e);
        }
        // 插入请求日志end
        return respJson;
    }

}

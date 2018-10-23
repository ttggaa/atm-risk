package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.dao.ThirdServiceCfgDao;
import com.risk.controller.service.dao.WanshuReqLogDao;
import com.risk.controller.service.entity.WanshuReqLog;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.service.WanshuService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 万树服务接口
 */
@Slf4j
@Service
public class WanshuServiceImpl implements WanshuService {

    @Autowired
    private WanshuReqLogDao wanshuReqLogDao;
    @Autowired
    private LocalCache localCache;

    @Override
    public WanshuReqLog getLogByPhone(WanshuReqLog wanshuReqLog) {
        return wanshuReqLogDao.getLogByPhone(wanshuReqLog);
    }

    @Override
    public int insert(WanshuReqLog wanshuReqLog) {
        try {
            return wanshuReqLogDao.insert(wanshuReqLog);
        } catch (Exception e) {
            log.error("插入数据失败：wanshuReqLog：{}", JSONObject.toJSONString(wanshuReqLog), e);
        }
        return 0;
    }

    @Override
    public WanshuReqLog queryKonghao(String nid, String phone) {
        WanshuReqLog query = new WanshuReqLog();
        query.setPhone(phone);
        query.setType(1);
        query = wanshuReqLogDao.getLogByPhone(query);
        if (null != query && StringUtils.isNotBlank(query.getStatus())) {
            return query;
        }

        String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.wanshu.konghao.url");
        String appId = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.wanshu.appId");
        String appKey = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.wanshu.appKey");

        Map<String, String> params = new HashMap<String, String>();
        params.put("appId", appId);
        params.put("appKey", appKey);
        params.put("mobile", phone);
        try {
            WanshuReqLog wanshuReqLog = new WanshuReqLog();
            wanshuReqLog.setNid(nid);
            wanshuReqLog.setPhone(phone);
            wanshuReqLog.setType(1);
            Long startTime = System.currentTimeMillis();
            String result = HttpClientUtils.doPost(url, params);
            Long cnt = System.currentTimeMillis() - startTime;
            wanshuReqLog.setReqCnt(cnt);
            JSONObject resultJson = JSONObject.parseObject(result);
            if (null != resultJson) {
                Integer chargeStatus = resultJson.getInteger("chargeStatus");
                wanshuReqLog.setChargesStatus(null == chargeStatus ? 0 : chargeStatus);

                String code = resultJson.getString("code");
                wanshuReqLog.setCode(StringUtils.isBlank(code) ? "" : code);

                JSONObject data = resultJson.getJSONObject("data");
                if (null != data) {
                    String status = data.getString("status");
                    wanshuReqLog.setStatus(StringUtils.isBlank(status) ? "" : status);
                }
                wanshuReqLog.setTag("");
                wanshuReqLog.setTradeNo("");
                wanshuReqLog.setAddTime(System.currentTimeMillis());
                this.insert(wanshuReqLog);
            } else {
                wanshuReqLog.setTradeNo("");
                wanshuReqLog.setStatus("");
                wanshuReqLog.setTag("");
            }
            return wanshuReqLog;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public WanshuReqLog yangmaodang(String nid, String phone) {
        WanshuReqLog query = new WanshuReqLog();
        query.setPhone(phone);
        query.setType(2);
        query = wanshuReqLogDao.getLogByPhone(query);
        if (null != query && StringUtils.isNotBlank(query.getStatus())) {
            return query;
        }

        WanshuReqLog wanshuReqLog = new WanshuReqLog();
        String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.wanshu.yangmaodang.url");
        String appId = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.wanshu.appId");
        String appKey = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.wanshu.appKey");
        try {
            Map<String, String> params = new HashMap<>();
            params.put("appId", appId);
            params.put("appKey", appKey);
            params.put("mobile", phone);
            params.put("ip", "");
            wanshuReqLog.setNid(nid);
            wanshuReqLog.setPhone(phone);
            wanshuReqLog.setType(2);
            Long startTime = System.currentTimeMillis();
            String result = HttpClientUtils.doPost(url, params);
            Long cnt = System.currentTimeMillis() - startTime;
            wanshuReqLog.setReqCnt(cnt);
            JSONObject resultJson = JSONObject.parseObject(result);
            if (null != resultJson) {
                Integer chargeStatus = resultJson.getInteger("chargeStatus");
                wanshuReqLog.setChargesStatus(null == chargeStatus ? 0 : chargeStatus);

                String code = resultJson.getString("code");
                wanshuReqLog.setCode(StringUtils.isBlank(code) ? "" : code);

                JSONObject data = resultJson.getJSONObject("data");
                if (null != data) {
                    String tradeNo = data.getString("tradeNo");
                    wanshuReqLog.setTradeNo(StringUtils.isBlank(tradeNo) ? "" : tradeNo);

                    String status = data.getString("status");
                    wanshuReqLog.setStatus(StringUtils.isBlank(status) ? "" : status);

                    String tag = data.getString("tag");
                    wanshuReqLog.setTag(StringUtils.isBlank(tag) ? "" : tag);
                } else {
                    wanshuReqLog.setTradeNo("");
                    wanshuReqLog.setStatus("");
                    wanshuReqLog.setTag("");
                }
                wanshuReqLog.setAddTime(System.currentTimeMillis());
                this.insert(wanshuReqLog);
            }
            return wanshuReqLog;
        } catch (Throwable e) {
            log.error("请求羊毛党失败，");
        }
        return null;
    }

}

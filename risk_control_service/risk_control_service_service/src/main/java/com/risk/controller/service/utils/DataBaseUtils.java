package com.risk.controller.service.utils;


import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.service.impl.LocalCache;
import com.risk.controller.service.utils.pki.RSAUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 大数据请求数据公共类
 */
@Component
@Slf4j
public class DataBaseUtils {

    public static final String DATABASE_SMS = "sms"; //数仓短信表名
    public static final String DATABASE_CONTACT = "contact";//数仓通讯录表名
    public static final String DATABASE_CALLRECORD = "callrecord";//数仓通话记录表名

    @Resource
    private LocalCache localCache;

    /**
     * 数据加密，加签
     *
     * @param reqData     请求参数
     * @param orgCode     机构代码（大数据提供）
     * @param channelCode 方法代码（大数据提供）
     * @param pubKey      公钥
     * @param priKey      私钥
     * @return 组装的参数
     * @throws Exception
     */
    public static Map<String, String> encryptData(String reqData, String orgCode, String channelCode, String pubKey, String priKey) throws Exception {
        String sign = null;
        try {
            // 调用方私钥签名
            sign = RSAUtils.sign("SHA1WithRSA", reqData.getBytes("UTF-8"), RSAUtils.readPrivateKey(priKey));

        } catch (Exception e) {
            log.error("签名失败：{}", e);
            e.printStackTrace();
            throw new Exception("签名失败");
        }
        try {
            // 秒白条公钥加密
            reqData = RSAUtils.encryptByPublicKey(reqData, RSAUtils.readPublicKey(pubKey), "UTF-8");
        } catch (Exception e) {
            log.error("加密失败：{}", e);
            e.printStackTrace();
            throw new Exception("加密失败,");
        }

        Map params = new HashMap<String, String>();
        params.put("reqData", reqData);
        params.put("orgCode", orgCode);
        params.put("channelCode", channelCode);
        params.put("sign", sign);
        return params;
    }

    /**
     * 数仓返回数据解密处理
     *
     * @param result 结果集
     * @param priKey 私钥
     * @return 大数据返回的结果
     */
    public static String decryData(String result, String priKey) {
        String respSign = result.split("\n")[0];
        String respDate = result.split("\n")[1];
        String data = RSAUtils.decryptByPrivateKey(respDate, RSAUtils.readPrivateKey(priKey), "utf-8");
        return data;
    }

    /**
     * 1、数据加签，加密
     * 2、发送请求
     * 3、解析返回数据，返回结果集
     *
     * @param url         请求url
     * @param reqData     请求数据（json格式）
     * @param orgCode     机构代码（大数据提供）
     * @param channelCode 方法代码（大数据提供）
     * @param pubKey      公钥
     * @param priKey      私钥
     * @return 大数据返回的结果
     * @throws Exception
     */
    public static String doPost(String url, String reqData, String orgCode, String channelCode, String pubKey, String priKey) {
        String result = "";
        try {
            Map<String, String> map = DataBaseUtils.encryptData(reqData, orgCode, channelCode, pubKey, priKey);
            result = HttpClientUtils.doPost(url, map);
        } catch (Throwable e) {
            log.error("请求大数据异常，url：{},data:{},error:{}", url, reqData, e);
            return null;
        }
        try {
            return DataBaseUtils.decryData(result, priKey);
        } catch (Throwable e) {
            log.error("解密失败，result：{},error:{}", result, e);
            return null;
        }
    }

    /**
     * 数仓查询数据结果集
     *
     * @param userId     用户id
     * @param type       数仓表名
     * @param param      查询参数(null：查询所有参数)
     * @param merchantId 商户ID
     * @return
     */
    public ResponseEntity queryByUserId(Long userId, String type, String param, String merchantId) {
        String pubKey = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "device.database.pubKey");
        String priKey = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "device.database.priKey");
        String orgCodes = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "device.database.orgCodes"); // 刷新缓存有效
        String queryUrl = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "device.database.getItems.url");

        param = StringUtils.isBlank(param) ? "*" : param;
        Map<String, String> params = new HashMap<>();
        params.put("query", "SELECT " + param + " FROM " + type + " where userId=" + userId);
        String reqData = JSONObject.toJSONString(params);

        JSONObject json = JSONObject.parseObject(orgCodes);
        if (null == json || !json.containsKey(merchantId) || StringUtils.isBlank(json.getString(merchantId))) {
            log.error("查询数据merchantId异常，merchantId：{}，userId：{}，type：{}，param：{}", new Object[]{merchantId, userId, type, param});
            return new ResponseEntity(ResponseEntity.STATUS_FAIL);
        }
        String orgCode = json.getString(merchantId);
        String channelCode = "TH0000004";

        String result = DataBaseUtils.doPost(queryUrl, reqData, orgCode, channelCode, pubKey, priKey);
        ResponseEntity rs = JSONObject.parseObject(result, ResponseEntity.class);
        log.debug("数仓返回数据：" + JSONObject.toJSONString(rs));
        if (null == rs) {
            log.error("查询数据失败");
            rs = new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "返回结果为null", null);
        }
        return rs;
    }


    /**
     * 1、数据加签，加密
     * 2、发送请求
     * 3、解析返回数据，返回结果集
     *
     * @param url     请求url
     * @param reqData 请求数据（json格式）
     * @return 大数据返回的结果
     * @throws Exception
     */
    public ResponseEntity doPostRiskLead(String url, String reqData) {

        String pubKey = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "risk-lead.pubKey");
        String priKey = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "risk-lead.priKey");
        String orgCode = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "risk-lead.orgCode"); // 刷新缓存有效

        String result = DataBaseUtils.doPost(url, reqData, orgCode, null, pubKey, priKey);
        ResponseEntity rs = JSONObject.parseObject(result, ResponseEntity.class);
        if (null == rs) {
            log.error("查询数据失败");
            rs = new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "返回结果为null", null);
        }
        return rs;
    }
}

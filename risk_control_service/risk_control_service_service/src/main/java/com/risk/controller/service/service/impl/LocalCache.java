package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.dao.RiskMerchantInfoDao;
import com.risk.controller.service.dao.RiskMerchantUrlDao;
import com.risk.controller.service.dao.SystemCfgDao;
import com.risk.controller.service.dao.ThirdServiceCfgDao;
import com.risk.controller.service.entity.RiskMerchantInfo;
import com.risk.controller.service.entity.RiskMerchantUrl;
import com.risk.controller.service.entity.SystemCfg;
import com.risk.controller.service.entity.ThirdServiceCfg;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 系统缓存
 *
 * @Author: Tonny
 * @CreateDate: 18/11/29 下午 04:54
 * @Version: 1.0
 */
@Component
public class LocalCache implements InitializingBean {
    @Resource
    private ThirdServiceCfgDao thirdServiceCfgDao;
    @Resource
    private SystemCfgDao systemCfgDao;
    @Resource
    private RiskMerchantInfoDao riskMerchantInfoDao;
    @Resource
    private RiskMerchantUrlDao riskMerchantUrlDao;

    private HashMap<String, Map<String, String>> cfg = null;


    /**
     * 启动后自动加载第三方配置到内存
     */
    public synchronized void initLoaclCache() {
        if (null == cfg) {
            cfg = new HashMap<String, Map<String, String>>();
            //加载第三方服务配置
            HashMap<String, String> thirdCfg = new HashMap<String, String>();
            List<ThirdServiceCfg> list = thirdServiceCfgDao.getAll();
            if (!CollectionUtils.isEmpty(list)) {
                for (Iterator<ThirdServiceCfg> it = list.iterator(); it.hasNext(); ) {
                    ThirdServiceCfg thirdServiceCfg = it.next();
                    thirdCfg.put(thirdServiceCfg.getName(), thirdServiceCfg.getValue());
                }
                //后面可能还有其它配置大项，故分开
                cfg.put(CacheCfgType.THIRDSERVICECFG.name(), thirdCfg);
            }

            //加载应用系统配置
            HashMap<String, String> systemCfg = new HashMap<String, String>();
            List<SystemCfg> systemCfgList = systemCfgDao.getAll();
            if (!CollectionUtils.isEmpty(systemCfgList)) {
                for (Iterator<SystemCfg> it = systemCfgList.iterator(); it.hasNext(); ) {
                    SystemCfg cfg = it.next();
                    systemCfg.put(cfg.getKey(), cfg.getValue());
                }
                //后面可能还有其它配置大项，故分开
                cfg.put(CacheCfgType.SYSTEMCFG.name(), systemCfg);
            }

            // 加载商户信息
            HashMap<String, String> merchantInfo = new HashMap<>();
            List<RiskMerchantInfo> listMerchantInfo = riskMerchantInfoDao.getAll();
            if (!CollectionUtils.isEmpty(listMerchantInfo)) {
                listMerchantInfo.forEach(info -> merchantInfo.put(info.getCode(), String.valueOf(info.getEnabled())));
                cfg.put(CacheCfgType.MERCHANT_INFO.name(), merchantInfo);
            }
            List<RiskMerchantUrl> listMerchantUrl = riskMerchantUrlDao.getEnableAll();
            if (!CollectionUtils.isEmpty(listMerchantUrl)) {
                listMerchantUrl.forEach(url -> merchantInfo.put(url.getMerchantCode() + "." + url.getKey(), url.getUrl()));
                cfg.put(CacheCfgType.MERCHANT_INFO.name(), merchantInfo);
            }
        }
    }

    /**
     * 指定读取模式和读取的类型获取本地缓存
     *
     * @param type
     * @return
     */
    public Map<String, String> getLocalCacheCfg(CacheCfgType type, String key) {
        GetCacheModel model = GetCacheModel.NO_FLUSH;
        if (GetCacheModel.FLUSH == model) {
            //同步开启
            synchronized (GetCacheModel.FLUSH) {
                if (CacheCfgType.SYSTEMCFG == type) {
                    Map<String, String> systemCfg = cfg.get(type.name());
                    if (null != systemCfg) {
                        SystemCfg systemCfgFind = new SystemCfg();
                        systemCfgFind.setKey(key);
                        SystemCfg systemCfgRs = systemCfgDao.getOne(systemCfgFind);
                        if (null != systemCfgRs) {
                            systemCfg.put(key, systemCfgRs.getValue());
                        }
                    }
                } else if (CacheCfgType.THIRDSERVICECFG == type) {
                    Map<String, String> thirdServiceCfg = cfg.get(type.name());
                    if (null != thirdServiceCfg) {
                        ThirdServiceCfg thirdServiceCfgFind = new ThirdServiceCfg();
                        thirdServiceCfgFind.setName(key);
                        ThirdServiceCfg thirdServiceCfgRs = thirdServiceCfgDao.getOne(thirdServiceCfgFind);
                        if (null != thirdServiceCfgRs) {
                            thirdServiceCfg.put(key, thirdServiceCfgRs.getValue());
                        }
                    }
                } else if (CacheCfgType.MERCHANT_INFO == type) {
                    Map<String, String> thirdServiceCfg = cfg.get(type.name());
                    if (null != thirdServiceCfg) {
                        RiskMerchantInfo merchantInfo = new RiskMerchantInfo();
                        merchantInfo.setCode(key);
                        merchantInfo = riskMerchantInfoDao.getOneByKey(key);
                        if (null != merchantInfo) {
                            thirdServiceCfg.put(key, JSONObject.toJSONString(merchantInfo));
                        }
                    }
                }
            }
        }

        return cfg.get(type.name());
    }

    /**
     * 查询对应缓存模块的key
     *
     * @param model
     * @param type
     * @param key
     * @return
     */
    public String getLocalCache(GetCacheModel model, CacheCfgType type, String key) {
        Map<String, String> cfg = getLocalCacheCfg(type, key);
        if (null != cfg) {
            return cfg.get(key);
        }
        return null;
    }

    public String getLocalCache(CacheCfgType type, String key) {
        return getLocalCache(GetCacheModel.NO_FLUSH, type, key);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initLoaclCache();
    }

    public synchronized void refresh() throws Exception {
        this.cfg = null;
        initLoaclCache();
    }

    /*public void setLocalCache(CacheCfgType type, String key, String value){
        Map<String,String> c = cfg.get(type.name());
        c.put(key, value);

        if(CacheCfgType.SYSTEMCFG.equals(type)){
        	SystemCfg cfgFind = new SystemCfg();
        	cfgFind.setKey(key);
            SystemCfg cfgRs = systemCfgDao.getOne(cfgFind);
            if(null == cfgRs){
            	cfgRs = new SystemCfg();
            	cfgRs.setKey(key);
            	cfgRs.setValue(value);
            	this.systemCfgDao.insert(cfgRs);
            }else{
            	cfgRs.setValue(value);
            	this.systemCfgDao.update(cfgRs);
            }
        }

        if(CacheCfgType.THIRDSERVICECFG.equals(type)){
        	ThirdServiceCfg cfgFind = new ThirdServiceCfg();
            cfgFind.setKey(key);
            ThirdServiceCfg cfgRs = thirdServiceCfgDao.getOne(cfgFind);
            if(null == cfgRs){
            	cfgRs = new ThirdServiceCfg();
            	cfgRs.setKey(key);
            	cfgRs.setValue(value);
            	this.thirdServiceCfgDao.insert(cfgRs);
            }else{
            	cfgRs.setValue(value);
            	this.thirdServiceCfgDao.update(cfgRs);
            }
        }
    }*/
    public Map<String, Map<String, String>> getAll() {
        return this.cfg;
    }
}

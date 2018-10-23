package com.risk.controller.service.service;

import com.risk.controller.service.entity.WanshuReqLog;
import org.springframework.stereotype.Service;

/**
 * 万树服务接口
 */
@Service
public interface WanshuService {

    /**
     * 通过手机号码查询万树
     *
     * @param log
     * @return
     */
    WanshuReqLog getLogByPhone(WanshuReqLog log);

    /**
     * 插入数据
     *
     * @param log
     * @return
     */
    int insert(WanshuReqLog log);

    /**
     * 插入数据
     *
     * @return
     */
    WanshuReqLog queryKonghao(String nid, String phone);

    WanshuReqLog yangmaodang(String nid, String phone);
}

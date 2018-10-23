package com.risk.controller.service.utils.paixu.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
public class ControllerBaseVO {

    /**
     * 机构id
     */
    private String merchantId;

    /**
     *风控系统分配给产品的编号
     */
    private String productId;

    /**
      * 访问标识
     */
    String appKey;

    /**
     * 时间戳
     */
    String timeStamp;

    /**
     * 签名结果
     */
    String sign;
}

package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class WanshuReqLog extends BaseEntity {
    private Integer type;//1万树空号，2万树羊毛党
    private Integer chargesStatus;//1收费,0不收费
    private String nid;//订单号
    private String phone;//手机号码
    private String code;//响应code码。200000：成功，其他失败
    private String status;//0空号,1实号,2停机,3库无,4沉默号,5风险号,W1白名单,B1黑名单,B2可信任度低,N未找到
    private Long reqCnt;//请求耗时
    private String tradeNo;//交易号
    private String tag;//标签属性
}

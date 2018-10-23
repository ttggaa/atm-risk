package com.risk.controller.service.util;

/**
 *
 * 所有准入规则处理器都要实现此接口, 方法名不限定, 但方法签名要求如下:
 * AdmissionResultDTO method(BorrowInfoDTO borrowInfo, AdmissionRuleDTO rule);
 *
 * Created by root on 6/2/16.
 */
public interface AdmissionHandler {

}

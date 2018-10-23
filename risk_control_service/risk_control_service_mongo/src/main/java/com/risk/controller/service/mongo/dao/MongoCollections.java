package com.risk.controller.service.mongo.dao;

import lombok.Data;

@Data
public class MongoCollections {
    public static final String DB_XINYAN_APPLY = "xinyan_apply"; //新颜申请雷达
    public static final String DB_XINYAN_BEHAVIOR = "xinyan_behavior";//新颜行为雷达
    public static final String DB_DEVICE_CALL_RECORD = "client_call_record";//设备通话记录
    public static final String DB_DEVICE_INFO = "client_info";//设备信息主表
    public static final String DB_DEVICE_CONTACT = "client_mall_list";//设备通讯录
    public static final String DB_DEVICE_SMS = "client_sms";//设备短信
    public static final String DB_OPERATOR_INFO = "operator_info";//运营商原始报告
    
    public static final String DB_USER_MAIN_CONTACT = "client_urgent_persons";//用户紧急联系人信息
    public static final String DB_SHUMEI_BLACK = "sm_black";//树美黑名单
    public static final String DB_SHUMEI_BORROWS = "sm_borrows";//树美多头借贷
    public static final String DB_SHUMEI_RECHARGES = "operator_recharges";//树美多头借贷
    public static final String DB_OPERATOR_ORIGINAL_DATA = "operator_original_data";//运营商原始数据
    public static final String DB_OPERATOR_MOJIE_INFO = "operator_mojie_report";//运营商原始报告
    public static final String DB_OPERATOR_SMS = "operator_smses_detail";//运营商短信
    
    public static final String DB_OPERATOR_CALLS_DETAIL = "operator_calls_detail";//运营商短信



}
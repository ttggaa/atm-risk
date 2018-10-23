package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;


@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ModelOperatorReport extends BaseEntity {
    private Long userId;//用户id
    private String nid;//订单号
    private String date;//日期,
    private Long allCallNumIn;//所有运营商打进次数
    private Long allCallNumOut;//所有运营商打出次数
    private Long allCallNumUn;//所有运营商未知次数
    private Long allCallTimeIn;//所有运营商打进时长
    private Long allCallTimeOut;//所有运营商打出时长
    private Long allCallTimeUn;//所有运营商未知时长
    private Long mainCallNumIn;//紧急联系人打进次数
    private Long mainCallNumOut;//紧急联系人打出次数
    private Long mainCallNumUn;//紧急联系人未知次数
    private Long mainCallTimeIn;//紧急联系人打进时长
    private Long mainCallTimeOut;//紧急联系人打出时长
    private Long mainCallTimeUn;//紧急联系人未知时长
    private Long allSmsNumIn;//所有运营商手机号码接收短信次数
    private Long allSmsNumOut;//所有运营商手机号码发送短信次数
    private Long allSmsNumUn;//所有运营商手机号码未知短信次数
    private Long contactSmsNumIn;//接收联系人发送的短信次数
    private Long contactSmsNumOut;//给联系人发送的短信次数
    private Long contactSmsNumUn;//未知短信次数
    private Long cntCallNumIn;// 通讯录与运营商打进次数
    private Long cntCallNumOut;//通讯录与运营商打出次数
    private Long cntCallNumUn;//通讯录与运营商未知次数
    private Long cntCallTimeIn;//通讯录与运营商打进时长
    private Long cntCallTimeOut;//通讯录与运营商打出时长
    private Long cntCallTimeUn;//通讯录与运营商未知时长

    public ModelOperatorReport() {
        this.allCallNumIn = 0L;
        this.allCallNumOut = 0L;
        this.allCallNumUn = 0L;
        this.allCallTimeIn = 0L;
        this.allCallTimeOut = 0L;
        this.allCallTimeUn = 0L;
        this.mainCallNumIn = 0L;
        this.mainCallNumOut = 0L;
        this.mainCallNumUn = 0L;
        this.mainCallTimeIn = 0L;
        this.mainCallTimeOut = 0L;
        this.mainCallTimeUn = 0L;
        this.allSmsNumIn = 0L;
        this.allSmsNumOut = 0L;
        this.allSmsNumUn = 0L;
        this.contactSmsNumIn = 0L;
        this.contactSmsNumOut = 0L;
        this.contactSmsNumUn = 0L;
        this.cntCallNumIn = 0L;
        this.cntCallNumOut = 0L;
        this.cntCallNumUn = 0L;
        this.cntCallTimeIn = 0L;
        this.cntCallTimeOut = 0L;
        this.cntCallTimeUn = 0L;
    }
}

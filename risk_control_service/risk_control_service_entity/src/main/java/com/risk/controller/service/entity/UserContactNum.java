package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class UserContactNum {
    private Long numCallIn30; //30天打进通话次数
    private Long numCallIn60;
    private Long numCallIn90;
    private Long numCallIn120;
    private Long numCallIn150;
    private Long numCallIn180;
    private Long numCallOut30; //30天打出通话次数
    private Long numCallOut60;
    private Long numCallOut90;
    private Long numCallOut120;
    private Long numCallOut150;
    private Long numCallOut180;
    private Long numCallUnKonw30; //30天未知通话次数
    private Long numCallUnKonw60;
    private Long numCallUnKonw90;
    private Long numCallUnKonw120;
    private Long numCallUnKonw150;
    private Long numCallUnKonw180;
    private Long timeCallIn30; //30天打进通话时长
    private Long timeCallIn60;
    private Long timeCallIn90;
    private Long timeCallIn120;
    private Long timeCallIn150;
    private Long timeCallIn180;
    private Long timeCallOut30; //30天打出通话时长
    private Long timeCallOut60;
    private Long timeCallOut90;
    private Long timeCallOut120;
    private Long timeCallOut150;
    private Long timeCallOut180;
    private Long timeCallUnKonw30; //30天未知通话时长
    private Long timeCallUnKonw60;
    private Long timeCallUnKonw90;
    private Long timeCallUnKonw120;
    private Long timeCallUnKonw150;
    private Long timeCallUnKonw180;

    private Long cntCallNumIn30; //30天打进通话次数
    private Long cntCallNumOut30;
    private Long cntCallNumUnKnow30;
    private Long cntCallTimeIn30;
    private Long cntCallTimeOut30;
    private Long cntCallTimeUnKnow30;

    private Long cntCallNumIn60; //60天打进通话次数
    private Long cntCallNumOut60;
    private Long cntCallNumUnKnow60;
    private Long cntCallTimeIn60;
    private Long cntCallTimeOut60;
    private Long cntCallTimeUnKnow60;

    private Long cntCallNumIn90; //90天打进通话次数
    private Long cntCallNumOut90;
    private Long cntCallNumUnKnow90;
    private Long cntCallTimeIn90;
    private Long cntCallTimeOut90;
    private Long cntCallTimeUnKnow90;

    private Long cntCallNumIn120; //120天打进通话次数
    private Long cntCallNumOut120;
    private Long cntCallNumUnKnow120;
    private Long cntCallTimeIn120;
    private Long cntCallTimeOut120;
    private Long cntCallTimeUnKnow120;

    private Long cntCallNumIn150; //150天打进通话次数
    private Long cntCallNumOut150;
    private Long cntCallNumUnKnow150;
    private Long cntCallTimeIn150;
    private Long cntCallTimeOut150;
    private Long cntCallTimeUnKnow150;

    private Long cntCallNumIn180; //180天打进通话次数
    private Long cntCallNumOut180;
    private Long cntCallNumUnKnow180;
    private Long cntCallTimeIn180;
    private Long cntCallTimeOut180;
    private Long cntCallTimeUnKnow180;


    private Long allSmsNumIn; //所有手机号码接收次数
    private Long allSmsNumOut;
    private Long allSmsNumUnKonow;

    private Long contactSmsNumIn;//通讯录手机号码接收次数
    private Long contactSmsNumOut;
    private Long contactSmsNumUnKonow;

    private Long continueDiffDays30;// 最近30天，连续未通话天数
    private Long interruptDiffDays30;// 最近30天，断续续未通话天数

    public UserContactNum() {
        this.numCallIn30 = 0L;
        this.numCallIn60 = 0L;
        this.numCallIn90 = 0L;
        this.numCallIn120 = 0L;
        this.numCallIn150 = 0L;
        this.numCallIn180 = 0L;
        this.numCallOut30 = 0L;
        this.numCallOut60 = 0L;
        this.numCallOut90 = 0L;
        this.numCallOut120 = 0L;
        this.numCallOut150 = 0L;
        this.numCallOut180 = 0L;
        this.numCallUnKonw30 = 0L;
        this.numCallUnKonw60 = 0L;
        this.numCallUnKonw90 = 0L;
        this.numCallUnKonw120 = 0L;
        this.numCallUnKonw150 = 0L;
        this.numCallUnKonw180 = 0L;
        this.timeCallIn30 = 0L;
        this.timeCallIn60 = 0L;
        this.timeCallIn90 = 0L;
        this.timeCallIn120 = 0L;
        this.timeCallIn150 = 0L;
        this.timeCallIn180 = 0L;
        this.timeCallOut30 = 0L;
        this.timeCallOut60 = 0L;
        this.timeCallOut90 = 0L;
        this.timeCallOut120 = 0L;
        this.timeCallOut150 = 0L;
        this.timeCallOut180 = 0L;
        this.timeCallUnKonw30 = 0L;
        this.timeCallUnKonw60 = 0L;
        this.timeCallUnKonw90 = 0L;
        this.timeCallUnKonw120 = 0L;
        this.timeCallUnKonw150 = 0L;
        this.timeCallUnKonw180 = 0L;
        this.allSmsNumIn = 0L;
        this.allSmsNumOut = 0L;
        this.allSmsNumUnKonow = 0L;
        this.contactSmsNumIn = 0L;
        this.contactSmsNumOut = 0L;
        this.contactSmsNumUnKonow = 0L;
        this.continueDiffDays30 = 0L;
        this.interruptDiffDays30 = 0L;


        this.cntCallNumIn30 = 0L;
        this.cntCallNumOut30 = 0L;
        this.cntCallNumUnKnow30 = 0L;
        this.cntCallTimeIn30 = 0L;
        this.cntCallTimeOut30 = 0L;
        this.cntCallTimeUnKnow30 = 0L;
        this.cntCallNumIn60 = 0L;
        this.cntCallNumOut60 = 0L;
        this.cntCallNumUnKnow60 = 0L;
        this.cntCallTimeIn60 = 0L;
        this.cntCallTimeOut60 = 0L;
        this.cntCallTimeUnKnow60 = 0L;
        this.cntCallNumIn90 = 0L;
        this.cntCallNumOut90 = 0L;
        this.cntCallNumUnKnow90 = 0L;
        this.cntCallTimeIn90 = 0L;
        this.cntCallTimeOut90 = 0L;
        this.cntCallTimeUnKnow90 = 0L;
        this.cntCallNumIn120 = 0L;
        this.cntCallNumOut120 = 0L;
        this.cntCallNumUnKnow120 = 0L;
        this.cntCallTimeIn120 = 0L;
        this.cntCallTimeOut120 = 0L;
        this.cntCallTimeUnKnow120 = 0L;
        this.cntCallNumIn150 = 0L;
        this.cntCallNumOut150 = 0L;
        this.cntCallNumUnKnow150 = 0L;
        this.cntCallTimeIn150 = 0L;
        this.cntCallTimeOut150 = 0L;
        this.cntCallTimeUnKnow150 = 0L;
        this.cntCallNumIn180 = 0L;
        this.cntCallNumOut180 = 0L;
        this.cntCallNumUnKnow180 = 0L;
        this.cntCallTimeIn180 = 0L;
        this.cntCallTimeOut180 = 0L;
        this.cntCallTimeUnKnow180 = 0L;
    }
}

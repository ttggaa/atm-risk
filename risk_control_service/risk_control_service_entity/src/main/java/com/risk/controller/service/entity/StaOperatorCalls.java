package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 运营商历史通话记录、运营商短信-（手机号码）数据汇总
 */
@Data
public class StaOperatorCalls {
    private Long id;
    private String nid;//订单号
    private Integer day;//时间（天）
    private Integer optCallNum;//运营商主叫次数-手机
    private Integer optCall_time;//运营商主叫时长-手机
    private Integer optCallManNum;//运营商主叫人次
    private Integer optCalledNum;//运营商被叫次数-手机
    private Integer optCalled_time;//运营商被叫时长-手机
    private Integer optCalledManNum;//运营商被叫人次
    private Integer optManNum;//运营商通话人次（主叫+被叫）-手机号码
    private Integer optSmsSendNum;//运营商短信发送个数-手机
    private Integer optSmsReceiveNum;//运营商短信接收个数-手机
    private Integer optSmsManNum;//运营商短信通讯人次
    private Integer cntCallNum;//通讯录主叫次数-手机
    private Integer cntCall_time;//通讯录主叫时长-手机
    private Integer cntCallManNum;//通讯录主叫人次-手机
    private Integer cntCalledNum;//通讯录被叫次数-手机
    private Integer cntCalled_time;//通讯录被叫时长-手机
    private Integer cntCalledManNum;//通讯录被叫人次-手机
    private Integer cntValidNum;//通讯录有效通话次数
    private Integer cntValid_time;//通讯录有效通话时长
    private Integer cntValidManNum;//通讯录有效通讯人次
    private Integer cntEachOtherNum;//互相通话次数-通讯录手机在运营商互通次数-手机
    private Integer cntEachOther_time;//互相通话时长-通讯录手机在运营商互通时长-手机
    private Integer cntEachOtherManNum;//互相通话人次-通讯录手机在运营商互通人次-手机
    private Integer addTime;//添加时间
    private Integer updateTime;//修改时间
}

package com.risk.controller.service.entity;

import lombok.Data;

@Data
public class OperatorCallRecord extends BaseEntity {
    private Long id;
    private Long userId;//用户id
    private String nid;//订单号
    private String phone;//用户手机号码
    private String peerNumber;//通话人手机号码
    private String locationType;//通话类型
    private Long duration;//通话时长
    private String time;//通话时间
}

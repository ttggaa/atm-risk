package com.risk.controller.service.entity;

import lombok.Data;

@Data
public class ClientContact extends BaseEntity {
    private Long id;
    private Long userId;//用户id
    private String nid;//订单号
    private String phone;//用户手机号码
    private String name;//联系人姓名
    private String contactsPhone;//联系人手机号码
}

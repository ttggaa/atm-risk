package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class RejectReason extends BaseEntity {
    private String name;//拒绝原因名称
    private Integer code;//原因码
    private Integer closeDays;//封闭天数
    private Integer riskLevel;//严重等级：取值1到6，值越大越严重
}

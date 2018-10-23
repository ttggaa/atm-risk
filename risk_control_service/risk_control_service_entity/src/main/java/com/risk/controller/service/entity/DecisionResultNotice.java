package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 模型到saas结果通知
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DecisionResultNotice extends BaseEntity {
    private String nid;
    private Integer status;
    private Integer result;
    private Integer noticeNum;
    private String rejectReasons;
    private String msg;
}

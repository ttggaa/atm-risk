package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class AdmissionResult extends BaseEntity {
    private String nid;//借款单号
    private Integer result;//执行结果
    private Long timeCost;//耗时
    private Long labelGroupId;//标签组Id
    private Integer failFast;//快速失败标识
    private Integer stopStage;//结束步骤
    private Long labelTimeCost;//打标签执行时间

    private Long suspendTime; //挂起时间
    private Integer suspendCnt;//本次挂起次数
    private Integer suspendStage;//挂起步骤
    private Long suspendRuleId;//挂起的规则ID
    private Integer robotAction; // 1-执行评分，9-跳过，
}

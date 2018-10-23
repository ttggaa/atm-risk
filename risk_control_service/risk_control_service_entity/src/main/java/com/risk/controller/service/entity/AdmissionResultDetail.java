package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class AdmissionResultDetail extends BaseEntity {
	private Long resultId; //AdmissionResult的id
	private Integer stage;
	private Long ruleId;
	private Integer result; //结果状态

	private String dataType;
	private String data;
	private Long timeCost;//耗时
	private String rejectReasonCode;//拒绝原因代码
	private Long suspendTime;//挂起时间
	private Integer suspendCnt;//挂起次数
	private Integer suspendStage;//挂起时所处步骤
}

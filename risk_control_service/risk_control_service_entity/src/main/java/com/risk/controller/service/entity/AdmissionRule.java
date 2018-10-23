package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Table;
import javax.persistence.Transient;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Table(name = "risk_decision_rule")
public class AdmissionRule extends BaseEntity {
	private String name;
	private String category;
	private String description;
	private Integer enabled;
	private Integer priority;
	private String handler;
	private String rejectReasonCode;
	private String setting;
	private Long maxSuspendTimeout;//最大挂起时长
	private Integer maxSuspendCnt;//最大挂起次数
	private Integer suspendResult;//规则挂起次数过多或超时返回结果

	@Transient
	private Integer robotAction;
}

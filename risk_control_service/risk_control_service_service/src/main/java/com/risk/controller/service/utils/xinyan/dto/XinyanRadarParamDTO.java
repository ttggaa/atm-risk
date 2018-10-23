package com.risk.controller.service.utils.xinyan.dto;

import lombok.Data;

/**
 * 新颜-雷达-业务请求参数DTO
 * created by fuyuling on 2018年7月10日
 */
@Data
public class XinyanRadarParamDTO {

	/** （必须）身份证号 */
	private String idNo;
	
	/** （必须）姓名 */
	private String idName;
	
	/** 手机号 */
	private String phoneNo;
	
	/** 银行卡号 */
	private String bankcardNo;
	
}

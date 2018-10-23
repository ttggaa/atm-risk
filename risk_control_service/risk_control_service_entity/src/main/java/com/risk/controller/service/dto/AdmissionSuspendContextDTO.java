package com.risk.controller.service.dto;

import com.risk.controller.service.entity.AdmissionResult;
import com.risk.controller.service.entity.AdmissionResultDetail;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

/**
 * Created by root on 6/2/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class AdmissionSuspendContextDTO {
    private AdmissionResult admissionResult;
    private Map<Long, AdmissionResultDetail> resultDetailMap; // key: ruleId
}

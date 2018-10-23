package com.risk.controller.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Created by root on 7/20/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DecisionGroupLabelDTO extends BaseDto {
    private Long groupId;
    private String labelIdCsv;
}

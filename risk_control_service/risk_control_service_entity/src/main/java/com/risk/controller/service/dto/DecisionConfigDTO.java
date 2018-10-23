package com.risk.controller.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Created by root on 6/2/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)

public class DecisionConfigDTO extends BaseDto {
    private Integer failFast; // 0/1
}

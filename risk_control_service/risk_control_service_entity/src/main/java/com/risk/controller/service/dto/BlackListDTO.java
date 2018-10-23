package com.risk.controller.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Created by root on 6/2/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class BlackListDTO extends BaseDto {
    private String phones;
    private String idCards;
    private String idfas;
    private String macs;
    private String imeis;
    private String source;//黑名单来源
}

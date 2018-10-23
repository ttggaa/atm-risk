package com.risk.controller.service.dto;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DeviceUsedDto extends BaseDto {
    private JSONArray imei;
    private JSONArray idfa;
    private JSONArray mac;
    private JSONArray ip;
}

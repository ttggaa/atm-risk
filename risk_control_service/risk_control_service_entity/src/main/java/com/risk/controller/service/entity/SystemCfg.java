package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Created by robot on 2016/4/19 0019.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
//@Table(name = "yhb_system_cfg")
public class SystemCfg extends BaseEntity {
    //@Column(name="`key`")
    private String key;
    private String value;
    private String remark;
}

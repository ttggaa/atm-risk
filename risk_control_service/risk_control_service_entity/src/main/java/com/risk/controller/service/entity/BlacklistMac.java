package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


/**
 * 黑名单-mac
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class BlacklistMac extends BaseEntity {
    private String mac;
    private Integer enable;//0停用，1启用
    private String source;//黑名单来源
    private String remark;//备注

}

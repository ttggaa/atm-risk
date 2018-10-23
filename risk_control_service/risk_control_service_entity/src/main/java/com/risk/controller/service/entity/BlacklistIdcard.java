package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


/**
 * 黑名单-身份证号码
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class BlacklistIdcard extends BaseEntity {
    private String idCard;//身份证号码
    private Integer enable;//0停用，1启用
    private String source;//黑名单来源
    private String remark;//备注

}

package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


/**
 * 地区黑名单（身份证号）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class BlacklistAreaIdcard extends BaseEntity {
    private String code;
    /**
     * 内部逾期客户/拍拍贷/手机贷
     */
    private String source;
    private Integer level;
    private String remark;
    private Integer enabled;
}

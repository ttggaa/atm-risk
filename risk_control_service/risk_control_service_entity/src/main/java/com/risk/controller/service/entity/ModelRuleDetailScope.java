package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ModelRuleDetailScope extends BaseEntity {
    public final static String SCOPEDAYS_30 = "day_30";
    public final static String SCOPEDAYS_60 = "day_60";
    public final static String SCOPEDAYS_90 = "day_90";
    public final static String SCOPEDAYS_120 = "day_120";
    public final static String SCOPEDAYS_150 = "day_150";
    public final static String SCOPEDAYS_180 = "day_180";

    public final static String SCOPE_48 = "hour_48";


    private Long ruleId; //规则id
    private String key;//区间天数，30,60,90，120,150,180
    private BigDecimal weight;//权重
    private Integer enabled; //1启用，0停用
    private String remark;//备注
    private Long num;//区间范围次数或者时长
}

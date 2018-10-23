package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ModelRuleDetail extends BaseEntity {

    /**
     * 通话次数主叫
     */
    public final static Long DETAIL_1001 = 100010011001L;//通话次数被叫
    public final static Long DETAIL_1002 = 100010011002L;//通话次数主叫
    public final static Long DETAIL_1003 = 100010011003L;//通话次数未知
    public final static Long DETAIL_1004 = 100010011004L;//通话时长主叫
    public final static Long DETAIL_1005 = 100010011005L;//通话时长被叫
    public final static Long DETAIL_1006 = 100010011006L;//通话时长未知
    public final static Long DETAIL_2001 = 100010021001L;//连续未通话/断续未通话
    public final static Long DETAIL_3001 = 100010031001L;//所有手机号码发送过短信
    public final static Long DETAIL_3002 = 100010031002L;//通讯录手机号码发送过短信

    public final static Long DETAIL_4001 = 100010041001L;//通讯录通话次数被叫
    public final static Long DETAIL_4002 = 100010041002L;//通讯录通话次数主叫
    public final static Long DETAIL_4003 = 100010041003L;//通讯录通话次数未知
    public final static Long DETAIL_4004 = 100010041004L;//通讯录通话时长主叫
    public final static Long DETAIL_4005 = 100010041005L;//通讯录通话时长被叫
    public final static Long DETAIL_4006 = 100010041006L;//通讯录通话时长未知


    private Long ruleId; //模型规则id
    private Long detailId; //模型对应的明细id
    private String name;//模型名称
    private BigDecimal weight;//模型权重
    private Integer enabled; //1启用，0停用
    private String remark;//备注
}

package com.risk.controller.service.entity;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户基本信息
 */
@Data
public class StaUserBaseinfo {
    private Long id;
    private String nid;//订单号
    private Integer age;//年龄
    private String inTime;//入网时间
    private Integer duration;//入网时长（月）
    private Integer optAvgFee;//平均话费（分）
    private Integer cntNum;//设备通讯录个数-手机号码
    private Integer cntRegisterNum;//通讯录注册人数
    private Integer optCallsRegisterNum;//运营商通话记录注册人数
    private Integer optShortNum;//运营商3位短号个数（非110,114,112,117,120,119,122,121,103,108,184,999）
    private Integer userDeviceNum;//用户使用设备数量
    private Integer userDeviceUsedNum;//用户的设备被多少人使用
    private Integer addTime;//添加时间
    private Integer updateTime;//修改时间

    public static final Set<String> shortNumSet = new HashSet<String>() {{ add("110"); add("114"); add("112"); add("117"); add("120"); add("119"); add("122"); add("121"); add("103");  add("108"); add("184"); add("999"); }};

}

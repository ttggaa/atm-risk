package com.risk.controller.service.dto;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.entity.StaOperatorCalls;
import com.risk.controller.service.entity.StaSmBorrows;
import com.risk.controller.service.entity.StaUserBaseinfo;
import lombok.Data;

import java.util.List;

@Data
public class RobotRequestDTO {
    private List<JSONObject> callRecords; // 临时保存通话记录
    private List<JSONObject> sms; //临时保存短信信息
    private List<JSONObject> contacts; //临时保存通讯录信息
    private List<JSONObject> mainContacts;//临时保存用户紧急联系人
    private List<JSONObject> operatorCallRecords;//临时保存运营商通话记录

    private JSONObject operatorInfo;//临时保存用户紧急联系人
    private JSONObject operatorReport; //临时保存运营商报告数据
    private JSONObject userDeviceInfo;//临时保存用户设备信息
    private String number; //临时保存设备批次号
    private JSONObject shuMeiMultipoint; //临时保存树美多头
    private String operatorNum; //临时保存运营商数据编号
    private String clientNum; //临时保存设备上信息数据编号

    private Integer deviceUsedCount;//设备被多人使用次数
    private Integer userDeviceCount;//申请人使用设备的个数
    private Integer userDeviceContactRegisterCount;//通讯录中注册用户
    private Integer userDeviceContacCount;//通讯录中联系人数量
    private Integer userShortNumCount;//短号个数（设备通讯录、设备通话记录。运营商通话记录）
    private Integer userOpertorPhoneUsedTime;//手机使用时长
    private Integer userOperatorAvgCharge;// 运营商平均话费
    private Integer userShumeiCount;//树美多头借贷个数
    private Integer robotKhYmdCount;//用户、紧急联系人手机号码空号、羊毛党验证

//    private Integer userCallNum10;					//10天内主叫次数-手机
//    private Integer userCallTime10;					//10天内主叫时长-手机
    private Integer userCalledNum10;				//10天内被叫次数-手机
    private Integer userCalledTime10;				//10天内被叫时长-手机
    private Integer userCallAndCalledNum10;			//10天内互通次数-手机
    private Integer userCallAndCalledTime10;		//10天内互通时长-手机
    private Integer userCallAndCalledContactNum10;	//10天内互通人次-手机
    private Integer userCallAndCalledPercent10;		//10天内通话时长和次数比值-手机
    private Integer userCallNum30;					//30天内主叫次数-手机
    private Integer userCallTime30;					//30天内主叫时长-手机
    private Integer userCalledNum30;				//30天内被叫次数-手机
    private Integer userCalledTime30;				//30天内被叫时长-手机
    private Integer userCallAndCalledNum30;			//30天内互通次数-手机
    private Integer userCallAndCalledTime30;		//30天内互通时长-手机
    private Integer userCallAndCalledContactNum30;	//30天内互通人次-手机
    private Integer userCallAndCalledPercent30;		//30天内通话时长和次数比值-手机
    private Integer userCallNum60;					//60天内主叫次数-手机
    private Integer userCallTime60;					//60天内主叫时长-手机
    private Integer userCalledNum60;				//60天内被叫次数-手机
    private Integer userCalledTime60;				//60天内被叫时长-手机
    private Integer userCallAndCalledNum60;			//60天内互通次数-手机
    private Integer userCallAndCalledTime60;		//60天内互通时长-手机
    private Integer userCallAndCalledContactNum60;	//60天内互通人次-手机
    private Integer userCallAndCalledPercent60;		//60天内通话时长和次数比值-手机

    private Integer modelNum;//1排序模型（），2:本地模型
    private Integer source;//1生产数据，2训练数据

    private StaUserBaseinfo staUserBaseinfo; //用户基础信息
    private StaSmBorrows staSmBorrows; //树美多头信息
    private List<StaOperatorCalls> listOperatorCalls; //用户运营商通话信息

}

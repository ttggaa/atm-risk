package com.risk.controller.service.entity;

import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class RiskXinyanLog extends BaseEntity {

    private Integer code;//请求代码
    private String idNo;//身份证号码
    private String idName;//身份证号码
    private Integer score;//分数
    private String url; //
    private String reqParam;// 请求参数
    private Long reqTime;// 请求时间
    private Long reqCnt;//请求耗时
    private String repParam; //响应结果
    private Integer status;//0:初始状态，1：请求成功，2请求失败
    private String transId;//请求订单号唯一
    private String repErrorcode;//错误代码


    public RiskXinyanLog(Integer code, String idNo, String idName, String url,String transId,
                         Long reqTime, Long reqCnt, Integer status, String reqParam, String repParam,Integer socre) {
        this.transId = transId;
        this.code = code;
        this.idNo = idNo;
        this.idName = idName;
        this.url = url;
        this.reqTime = reqTime;
        this.reqCnt = reqCnt;
        this.status = status;
        if (StringUtils.isNotBlank(reqParam)) {
            if (reqParam.length() > 1000) {
                reqParam = reqParam.substring(0, 1000);
            }
        } else {
            reqParam = "";
        }
        this.reqParam = reqParam;
        if (StringUtils.isNotBlank(repParam)) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");//去除字符串中的空格、tab、回车、换行符
            Matcher m = p.matcher(repParam);
            repParam = m.replaceAll("");
            if (repParam.length() > 1000) {
                repParam = repParam.substring(0, 1000);
            }
        } else {
            repParam = "接口返回空";
        }
        this.score = socre;
        this.repParam = repParam;
    }

    public RiskXinyanLog() {
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }
}

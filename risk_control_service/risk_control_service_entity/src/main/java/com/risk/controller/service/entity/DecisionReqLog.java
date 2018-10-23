package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class DecisionReqLog extends BaseEntity {
    private String nid;//借款单号
    private String reqData;//执行结果

    public DecisionReqLog(String nid, String reqData) {
        this.nid = nid;
        if (StringUtils.isNotBlank(reqData)) {
            if (reqData.length() > 4000) {
                reqData = reqData.substring(0, 4000);
            }
        } else {
            reqData = "";
        }
        this.reqData = reqData;
        Long time = System.currentTimeMillis();
        this.addTime = time;
        this.updateTime = time;
    }

    public DecisionReqLog() {
    }
}

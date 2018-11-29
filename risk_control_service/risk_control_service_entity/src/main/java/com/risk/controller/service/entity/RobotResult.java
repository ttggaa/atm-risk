package com.risk.controller.service.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RobotResult {
    private Long id;
    private String nid;//订单号
    private BigDecimal num;//规则对应的个数
    private BigDecimal score;//用户得分
    private BigDecimal totalScore;//总分
    private Integer result;//是否满足，1满足，0不满足
    private Integer source;//来源1正式数据，2训练数据
    private Long addTime;
    private Long updateTime;

    public static Integer SOURCE_1 = 1; // 1正式数据，查询生产模型区间
    public static Integer SOURCE_2 = 2; // 2训练数据，查询训练模型区间
    public static Integer SOURCE_3 = 3; // 3训练数据，查询生产模型区间

    public RobotResult() {
    }

    public RobotResult(String nid, BigDecimal num, Integer result, Integer source, BigDecimal score, BigDecimal totalScore) {
        this.nid = nid;
        this.num = num;
        this.score = score;
        this.totalScore = totalScore;
        this.result = result;
        if (null == source) {
            source = RobotResult.SOURCE_1;
        }
        this.source = source;
    }
}

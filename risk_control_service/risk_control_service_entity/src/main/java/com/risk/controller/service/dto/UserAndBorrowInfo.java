package com.risk.controller.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户和订单的信息类
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAndBorrowInfo implements Serializable {
    // 订单相关
    private String nid;
    private BigDecimal amount;
    private Long productId;
    private Long applyTime;
    private Integer lastBorrowStatus;
    // 用户相关
    private String userName;
    private Integer successCount;
    private Integer maxDelinquentDays;
    private String cardId;
    private String mainContact1Phone;
    private String mainContact2Phone;
    private String mainContact1Relation;
    private String mainContact2Relation;

    private String name; // 用户真实姓名
    private String userBankNo;
}

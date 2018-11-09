package com.risk.controller.service.entity;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户基本信息
 */
@Data
public class StaSmBorrows {
    private Long id;
    private String nid;//订单号',
    private Integer applications180d;//180在多少不同网贷平台提出过申请',
    private Integer applications90d;//90在多少不同网贷平台提出过申请',
    private Integer applications60d;//60在多少不同网贷平台提出过申请',
    private Integer applications30d;//30在多少不同网贷平台提出过申请',
    private Integer applications7d;//7在多少不同网贷平台提出过申请',
    private Integer applications;//在多少不同网贷平台提出过申请',
    private Integer approvals180d;//
    private Integer approvals90d;//
    private Integer approvals60d;//
    private Integer approvals30d;//
    private Integer approvals7d;//
    private Integer approvals;//
    private Integer queries180d;//
    private Integer queries90d;//
    private Integer queries60d;//
    private Integer queries30d;//
    private Integer queries7d;//
    private Integer queries;//
    private Integer refuses180d;//180在多少不同网贷平台被拒绝',
    private Integer refuses90d;//90在多少不同网贷平台被拒绝',
    private Integer refuses60d;//60在多少不同网贷平台被拒绝',
    private Integer refuses30d;//30在多少不同网贷平台被拒绝',
    private Integer refuses7d;//7在多少不同网贷平台被拒绝',
    private Integer refuses;//在多少不同网贷平台被拒绝',
    private Integer registers180d;//
    private Integer registers90d;//
    private Integer registers60d;//
    private Integer registers30d;//
    private Integer registers7d;//
    private Integer registers;//
}

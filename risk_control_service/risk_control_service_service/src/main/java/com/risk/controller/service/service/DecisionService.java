package com.risk.controller.service.service;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.RobotResultRequest;
import com.risk.controller.service.dto.RobotScoreDTO;

/**
 * ${DESCRIPTION}
 *
 * @Author ZT
 * @create 2018-08-27
 */
public interface DecisionService {


    /**
     * 通知业务系统风控结果
     * @param nid
     * @param merchantCode
     * @param admResult
     * @return
     */
    ResponseEntity noticeBorrowResultHandle(String nid, String merchantCode, AdmissionResultDTO admResult);

    /**
     * 重复通知业务系统风控结果
     * @param noticeNum
     * @param nid
     * @return
     */
    ResponseEntity pushRiskResult(String nid,Integer noticeNum);
}

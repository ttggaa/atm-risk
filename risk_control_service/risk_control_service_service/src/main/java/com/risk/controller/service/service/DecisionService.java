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
     * 模型执行结果通知
     * @param request
     * @param robotScore
     * @param isModelNotice
     */
    ResponseEntity robotResultNotice(RobotResultRequest request, RobotScoreDTO robotScore, boolean isModelNotice);

    /**
     * 根据nid获取对应的数据
     * @param nid
     * @return
     */
    ResponseEntity getAllDataByNid(String nid);

    /**
     * 通知业务系统风控结果
     * @param nid
     * @param admResult
     * @return
     */
    ResponseEntity noticeBorrowResultHandle(String nid, AdmissionResultDTO admResult);

    /**
     * 重复通知业务系统风控结果
     * @param noticeNum
     * @param nid
     * @return
     */
    ResponseEntity pushRiskResult(String nid,Integer noticeNum);


    /**
     * 定时任务重复通知业务系统风控结果
     * @return
     */
    ResponseEntity pushRiskResult();
}

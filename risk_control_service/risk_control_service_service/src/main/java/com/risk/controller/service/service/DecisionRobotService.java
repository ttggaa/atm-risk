package com.risk.controller.service.service;

import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.RobotResultRequest;
import com.risk.controller.service.dto.RobotScoreDTO;
import com.risk.controller.service.entity.DecisionRobotResult;
import com.risk.controller.service.request.DecisionHandleRequest; /**
 * 机器人打分的业务层接口
 *
 * @Author ZT
 * @create 2018-08-27
 */
public interface DecisionRobotService {
    RobotScoreDTO getRobotScore(DecisionHandleRequest request, boolean isSaveInput, boolean isSaveOut);

    /**
     * 保存机器人打分结果
     * @param rsd
     */
    void saveOut(RobotScoreDTO rsd);

    DecisionRobotResult merge(AdmissionResultDTO admResult, RobotScoreDTO robotScore, RobotResultRequest request);

    /**
     * 保存 DecisionRobotResult
     * @param decisionRobotResult
     */
    void saveDecisionRobotResult(DecisionRobotResult decisionRobotResult);
}

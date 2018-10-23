package com.risk.controller.service.dto;

import com.risk.controller.service.entity.AdmissionResult;
import com.risk.controller.service.entity.AdmissionResultDetail;
import com.risk.controller.service.entity.DecisionResultLabel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Set;

/**
 * Created by root on 6/2/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class AdmissionResultDTO extends BaseDto {
    public final static int RESULT_APPROVED = 1; // 正常, 继续执行(参考其他)
    public final static int RESULT_REJECTED = 2; // 拒绝
    public final static int RESULT_EXCEPTIONAL = 3; // 执行出现异常
    public final static int RESULT_FINAL_APPROVED = 4; //建议直接通过(如果没有拒绝, 没有异常)
    public final static int RESULT_MANUAL = 5; // 人工审核(如果没有拒绝, 没有异常)
    public final static int RESULT_SKIP = 6; // 查无此数据 跳过处理
    public final static int RESULT_SUSPEND = 99; // 挂起
    public final static int WAIT_ROBOT = 98;//等待评分模型

    public final static int ROBOT_ACTION_SCORE = 1; // 模型动作-评分
    public final static int ROBOT_ACTION_SKIP = 9; // 模型动作-跳过

    private int result;

    private int approvedCount = 0; // 通过规则个数
    private int rejectedCount = 0; // 拒绝规则个数
    private int exceptionalCount = 0; // 异常规则个数
    private int finalApprovedCount = 0; // 建议进终审规则个数
    private int manualCount = 0; // 建议人工审核规则个数
    private int suspendCount = 0;//挂起个数
    private Long labelGroupId;

    private Set<String> rejectReason; // reason code, 取自表 miaobt_reject_reason

    private Object data; //自定义返回数据

    private Set<DecisionResultLabel> labels; //决策输出的标签

    private List<AdmissionResultDetail> resultDetail;

    private AdmissionResultDetail suspendDetail;

    private AdmissionResult admissionResult;//新加的
    private List<Object> resultDetails;//新加的

    private Integer robotAction; // 1-执行评分，9-跳过，

    public void populate(AdmissionResultDTO other){
        if(null == other){
            return;
        }

        this.result = other.getResult();
        this.approvedCount = other.approvedCount;
        this.rejectedCount = other.rejectedCount;
        this.exceptionalCount = other.exceptionalCount;
        this.finalApprovedCount = other.finalApprovedCount;
        this.manualCount = other.manualCount;
        this.suspendCount = other.suspendCount;
        this.rejectReason = other.getRejectReason();
        this.data = other.getData();
        this.labels = other.getLabels();
        this.resultDetail = other.getResultDetail();
    }

    /**
     * 根据approvedCount, rejectedCount, exceptionalCount, finalApprovedCount, manualCount 设置result
     */
    public void setResultByRuleResult(){
        if (suspendCount > 0){
            this.setResult(AdmissionResultDTO.RESULT_SUSPEND);
        }else {
            if (rejectedCount > 0) { //拒绝
                this.setResult(AdmissionResultDTO.RESULT_REJECTED);
            } else {
                if (exceptionalCount > 0) { //异常
                    this.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
                } else {
                    if (manualCount > 0) { // 人工
                        this.setResult(AdmissionResultDTO.RESULT_MANUAL);
                    } else {
                        if (finalApprovedCount > 0) { //直接通过
                            this.setResult(AdmissionResultDTO.RESULT_FINAL_APPROVED);
                        } else {
                            this.setResult(AdmissionResultDTO.RESULT_APPROVED); // 继续
                        }
                    }
                }
            }
        }
    }
}

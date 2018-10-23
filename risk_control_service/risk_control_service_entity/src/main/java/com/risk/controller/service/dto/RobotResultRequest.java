package com.risk.controller.service.dto;

import lombok.Data;

/**
 * @Project miaobt-approve-parent
 * @Class RobotResultDto
 * TODO
 * @Version 1.0
 * @Date 2017/8/4 10:10
 * @Author pc
 */
@Data
public class RobotResultRequest {
    //{'prob_of_0': 0.33589005470275879, 'final': 1, 'prob_of_1': 0.66410995, 'preliminary': 1}
    private String nid;
    private String prob_of_0 ;
    private String finalResult;
    private String prob_of_1;
    private String preliminaryResult;
    private Integer status;
}

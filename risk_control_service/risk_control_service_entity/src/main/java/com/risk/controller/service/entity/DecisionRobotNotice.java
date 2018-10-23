package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Created by Administrator on 2016/11/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DecisionRobotNotice extends BaseEntity {
    private String nid;
    private Integer status;
    private Integer noticeNum;
    private Integer exptStatus;
}

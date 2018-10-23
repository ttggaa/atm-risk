package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Table(name = "miaobt_decision_label_group")
public class DecisionLabelGroup extends BaseEntity {
    private String name;
    private Integer stageCount;
    private String remark;
    private Integer enabled; // 是否在用，1-是

}

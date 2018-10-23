package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Transient;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class DecisionResultLabel extends BaseEntity {
    private Long resultId;
    private Long labelId;
    private Long timeCost;

    @Transient
    private DecisionLabel decisionLabel;

    public static DecisionResultLabel fromDecisionLabel(DecisionLabel label){
        if(null == label){
            return null;
        }
        DecisionResultLabel ret = new DecisionResultLabel();
        ret.setLabelId(label.getId());
        ret.setDecisionLabel(label);
        return ret;
    }
}

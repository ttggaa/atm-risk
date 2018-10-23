package com.risk.controller.service.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ModelGroup extends BaseEntity {
    private Long groupId; //模型规则id
    private BigDecimal totalSocre; //模型对应的明细id
    private BigDecimal passScore;//模型名称
    private String remark;//备注
    private Integer enabled;//1启用，0停用
}

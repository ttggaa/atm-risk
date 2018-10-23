package com.risk.controller.service.enums;

import lombok.Getter;

/**
 * 黑名单的枚举
 */
public enum PhoneBlacklistStatus {
    SKIP(-1), // 秒白条查询无此数据,跳过
    THROUGH(0), // 通过
    NOTTHROUGH(1); // 不通过

    @Getter
    private Integer value;
    PhoneBlacklistStatus(Integer value){
        this.value = value;
    }
}

package com.risk.controller.service.enums;

/**
 * 标签Code的枚举
 */
public enum DecisionLabelCode {
    OLDLABEL("old"), // 老户
    SNEWLABEL("snew"), // 次新户
    NEWLABEL("new"), // 新户
    SECURED("secured"), // 有担保
    UNSECURED("unsecured"), // 无担保
    DEFAULT("default"), // 默认
    PASS("pass"),
    REJECT("reject"),
    ;

    private String type;
    private DecisionLabelCode(String type) {
        this.type = type;
    }
    @Override
    public String toString() {
        return type;
    }
}

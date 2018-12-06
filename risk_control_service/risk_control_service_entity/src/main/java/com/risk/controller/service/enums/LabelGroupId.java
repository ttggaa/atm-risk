package com.risk.controller.service.enums;

/**
 * 标签组id
 *
 * @Author: Tonny
 * @CreateDate: 18/11/30 下午 03:19
 * @Version: 1.0
 */
public enum LabelGroupId {
    /**
     * 新户组id
     */
    NEW(1000L),

    /**
     * 老户组id
     */
    OLD(1009L),
    /**
     * 通过组id
     */
    PASS(1001L),
    /**
     * 拒绝组id
     */
    REJECT(1002L),
    ;

    private Long type;

    LabelGroupId(Long type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.valueOf(type);
    }
    public Long value() {
        return type;
    }
}

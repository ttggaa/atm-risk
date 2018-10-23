package com.risk.controller.service.enums;

/**
 * Created by robot on 2016/4/16 0016.
 */
public enum GetCacheModel {
    FLUSH("强制刷新缓存"),
    NO_FLUSH("不刷新"),
    ;

    private String type;
    private GetCacheModel(String type) {
        this.type = type;
    }
    @Override
    public String toString() {
        return type;
    }
}

package com.risk.controller.service.enums;

/**
 * Created by robot on 2016/4/16 0016.
 */
public enum CacheCfgType {
    THIRDSERVICECFG("第三方服务配置"),
    SYSTEMCFG("系统配置"),
    ;

    private String type;
    private CacheCfgType(String type) {
        this.type = type;
    }
    @Override
    public String toString() {
        return type;
    }
}

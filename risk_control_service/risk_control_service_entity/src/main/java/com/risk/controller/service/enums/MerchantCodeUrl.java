package com.risk.controller.service.enums;

/**
 * description
 *
 * @Author: Tonny
 * @CreateDate: 18/12/19 下午 02:48
 * @Version: 1.0
 */
public enum MerchantCodeUrl {
    CALL_BACK("call_back_url"),
    USED_NUM("device_used_num_url"),
    REGISTER_NUM("register_num_url"),
    DEVICE_NUM("user_device_num_url"),
    USER_INFO("user_info_url"),
    OVERDUE_NUM("overdue_num_url"),
    REPEAT_OPERATOR("repeat_operator_url"),;

    private String type;

    MerchantCodeUrl(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.valueOf(type);
    }

    public String value() {
        return type;
    }

    public static void main(String[] args) {
        System.out.println(MerchantCodeUrl.CALL_BACK.value());
        System.out.println(MerchantCodeUrl.DEVICE_NUM);
    }
}

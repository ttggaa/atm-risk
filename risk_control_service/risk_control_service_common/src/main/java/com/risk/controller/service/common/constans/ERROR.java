package com.risk.controller.service.common.constans;

/**
 * Created by pc on 2016/8/5.
 */
public interface ERROR {
    interface ErrorCode {
        String SERVER_INTERNAL_ERROR = "E00090000";
        String REQUEST_PARAMS_ERROR = "E05031001";
        String PHONE_FORMAT_ERROR = "E05031002";
        String CARD_ID_ERROR = "E05031003";
        String NOTICE_BORROW_RESULT = "E05031004";
        String USER_BORROW_INFO = "E05031005";
        String THIRD_PARTY_CREDIT = "E05031006";
        String NOT_GROUP = "E05031007";
        String PARAMS_LACK = "E06000001";
    }
    

    interface ErrorMsg {
        String SERVER_INTERNAL_ERROR = "服务异常,请稍候再试!";
        String PHONE_FORMAT_ERROR = "用户手机号格式不正确";
        String CARD_ID_ERROR = "身份证号不合法";
        String NOTICE_BORROW_RESULT = "提醒borrow异常";
        String USER_BORROW_INFO = "根据业务订单号未获取到用户和订单信息异常";
        String THIRD_PARTY_CREDIT = "获取用户第三方增信信息异常";
        String NOT_GROUP = "未找到对应的分组";
        String PARAMS_LACK = "参数不足";
    }
}

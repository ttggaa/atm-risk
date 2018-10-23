/*
 * Copyright (C) 2016-2020 IMassBank Corporation
 *
 */
package com.risk.controller.service.utils.paixu;

/**
 * 常数类: 通用
 *
 * @author jinhongfei
 */
public final class CommonConstant {

    /**
     * 正则表达式: 只是英文
     */
    public static final String REGEX_ONLY_ENGLISH = "/^[a-zA-Z]+$/";

    /**
     * 正则表达式: 包含英文
     */
    public static final String REGEX_ENGLISH = "/[a-zA-Z]/";

    /**
     * 正则表达式: 英文和数字
     */
    public static final String REGEX_ENGLISH_NUMBER = "/[a-zA-Z0-9]/";

    /**
     * 正则表达式: 只包含数字
     */
    public static final String REGEX_ONLY_NUMBER = "^\\d+$";

    /**
     * 正则表达式: 只包含正整数（不包含0）
     */
    public static final String REGEX_POSITIVE_INTEGER = "^[1-9]\\d*$";

    /**
     * 正则表达式: 只包含非负整数（包含0）
     */
    public static final String REGEX_NON_NEGATIVE_INTEGER = "^[1-9]\\d*|0$";

    /**
     * 正则表达式: 小数
     */
    public static final String REGEX_DECIMAL = "^[-]?[0-9]+(\\.[0-9]+)?$";

    /**
     * 正则表达式: 非负浮点数（正浮点数 + 0）
     */
    public static final String REGEX_NON_NEGATIVE_DECIMAL = "^\\d+(\\.\\d+)?$";

    /**
     * 正则表达式: 身份证15位
     */
    public static final String REGEX_ID_NUMBER_15 = "\\d{15}";

    /**
     * 正则表达式: URL
     */
    public static final String REGEX_URL = "/^(https?|ftp)(:\\/\\/[-_.!~*\\'()a-zA-Z0-9;\\/?:\\@&=+\\$,%#]+)$/";

    /**
     * 正则表达式: 身份证18位
     */
    public static final String REGEX_ID_NUMBER_18 = "^[1-9][0-9]{5}(19|20)[0-9]{2}((01|03|05|07|08|10|12)((0[1-9]|[1-2][0-9])|31|30)|(04|06|09|11)(0[1-9]|[1-2][0-9]|30)|02(0[1-9]|[1-2][0-9]))[0-9]{3}([0-9]|x|X)$";

    /**
     * 正则表达式: 电话格式
     */
    public static final String REGEX_MOBILE = "^(1)(3[0-9]|4[5,7]|5[0-3,5-9]|7[0,3,6-8]|8[0-9])([0-9]{8})";

    /**
     * 正则表达式: 邮件格式
     */
    public static final String REGEX_EMAIL = "/^([a-zA-Z0-9])+([a-zA-Z0-9\\._-])*@([a-zA-Z0-9_-])+([a-zA-Z0-9\\._-]+)+$/";

    /**
     * 编码: UTF-8
     */
    public static final String CHARSET_UTF = "UTF-8";

    /**
     * 传输类型: JSON
     */
    public static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * 传输类型: text/plain
     */
    public static final String PLAIN_TEXT_TYPE = "text/plain";

    /**
     * 传输类型: 表单参数
     */
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    /**
     * 传输类型: 表单参数
     */
    public static final String CONTENT_TYPE_FORM_DATA = "multipart/form-data";

    /**
     * 传输类型: 表单参数(x-www-form-urlencoded)
     */
    public static final String CONTENT_TYPE_X_WWW_FORM_URL_ENCODED = "application/x-www-form-urlencoded";

    public static final String BASE_REQUEST_TIMEFORMAT = "yyyy-MM-dd HH:mm:ss";

}

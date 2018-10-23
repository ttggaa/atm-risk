package com.risk.controller.service.common.utils;

import org.apache.commons.lang.StringUtils;

public class PhoneUtils {
    public static String cleanMobile(String str) {
        if (null == str) {
            return null;
        }

        str = str.trim();
        str = str.replaceAll("[^\\d]", "");

        if (str.length() >= 13) {
            str = str.replaceAll("^86", "");
        }
        if (str.length() >= 16) {
            str = str.replaceAll("^(17950|17951|12593|17911|17901|17909)", "");
        }

        return str;
    }

    public static boolean isMobile(String str) {
        if (null == str || str.isEmpty()) {
            return false;
        }

        str = str.trim();

        return str.matches("^(?:13|14|15|16|17|18|19)\\d{9}");
    }

    public static String cleanTel(String str) {
        if (null == str) {
            return null;
        }

        str = str.trim();
        str = str.replaceAll("[^\\d]", "");

        return str;
    }

    /**
     * clean first, then compare
     *
     * @param phone1
     * @param phone2
     * @return
     */
    public static boolean isMobileEqual(String phone1, String phone2) {
        phone1 = (null == phone1) ? "" : phone1;
        phone2 = (null == phone2) ? "" : phone2;

        phone1 = cleanMobile(phone1);
        phone2 = cleanMobile(phone2);

        return phone1.equals(phone2);
    }

    /**
     * 验证手机号码尾号是否连号
     *
     * @param phone 手机号码
     * @return 连号个数
     */
    public static int checkPhoneContinuous(String phone) {
        int length = 1;
        char[] nums = phone.toCharArray();
        for (int i = nums.length - 1; i > 0; i--) {
            if (nums[i] == nums[i - 1]) {
                length++;
            } else {
                break;
            }
        }
        return length;
    }
}
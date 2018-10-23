package com.risk.controller.service.common.utils;

import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @Author ZT
 * @create 2018-08-27
 */
public class DateConvert implements Converter {
    public static final Logger LOGGER = LoggerFactory.getLogger(DateConvert.class);
    private static String dateFormatStr = "yyyy-MM-dd";
    private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat(dateFormatStr);

    private static String dateLongFormatStr = dateFormatStr + " HH:mm:ss";
    private static SimpleDateFormat dateTimeLongFormat = new SimpleDateFormat(dateLongFormatStr);

    public Object convert(Class arg0, Object arg1) {
        if (arg1 == null) {
            return null;
        }
        LOGGER.info(arg1.getClass().getName() + "=" + arg1.toString());
        String className = arg1.getClass().getName();
        //java.sql.Timestamp,java.util.Date,java.sql.Date
        if ("java.sql.Timestamp".equalsIgnoreCase(className)
                || "java.util.Date".equalsIgnoreCase(className)
                || "java.sql.Date".equalsIgnoreCase(className)) {
            try {
                SimpleDateFormat df = new SimpleDateFormat(dateFormatStr + " HH:mm:ss");
                return df.parse(dateTimeLongFormat.format(arg1));
            } catch (Exception e) {
                try {
                    SimpleDateFormat df = new SimpleDateFormat(dateFormatStr);
                    return df.parse(dateTimeFormat.format(arg1));
                } catch (ParseException ex) {
                    e.printStackTrace();
                    return null;
                }
            }
        } else {//Java.lang.String
            String p = (String) arg1;
            if (p == null || p.trim().length() == 0) {
                return null;
            }
            try {
                SimpleDateFormat df = new SimpleDateFormat(dateFormatStr + " HH:mm:ss");
                return df.parse(p.trim());
            } catch (Exception e) {
                try {
                    SimpleDateFormat df = new SimpleDateFormat(dateFormatStr);
                    return df.parse(p.trim());
                } catch (ParseException ex) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
    }

    public static String formatDateTime(Object obj) {
        if (obj != null)
            return dateTimeFormat.format(obj);
        else
            return "";
    }

    public static String formatLongDateTime(Object obj) {
        if (obj != null)
            return dateTimeLongFormat.format(obj);
        else
            return "";
    }


    public static String convertLongToDateTime(Long time, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date dt = new Date(time * 1000);
        String sDateTime = sdf.format(dt);
        return sDateTime;
    }

    public static Long formatStrDateTime2Long(String dateTime, String format) throws Exception {
        if (StringUtils.isBlank(dateTime)) {
            throw new Exception("日期时间不能为空!");
        }
        if (StringUtils.isBlank(format)) {
            throw new Exception("日期格式不能为空!");
        }


        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = null;
        try {
            date = df.parse(dateTime);
        } catch (ParseException e) {
            throw new Exception(e.toString());
        }
        return date.getTime() / 1000;
    }

    public static Date formatStrDate(String dateTime, String format) throws Exception {
        if (StringUtils.isBlank(dateTime)) {
            throw new Exception("日期时间不能为空!");
        }
        if (StringUtils.isBlank(format)) {
            throw new Exception("日期格式不能为空!");
        }


        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = null;
        try {
            date = df.parse(dateTime);
        } catch (ParseException e) {
            throw new Exception(e.toString());
        }
        return date;
    }

    public static void main(String[] args) {
        String dateTime = DateConvert.formatLongDateTime(new Date());
        try {
            Long time = DateConvert.formatStrDateTime2Long(dateTime, "yyyy-MM-dd HH:mm:ss");

            System.out.println(DateConvert.formatLongDateTime(new Date(time)));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 计算d1和d2时间差（d1-d2）
     *
     * @param d1 开始时间
     * @param d2 结束时间
     * @return 返回相差几个月
     */
    public static int getMonthDiff(Date d1, Date d2) {
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(d1);
        c2.setTime(d2);
        if (c1.getTimeInMillis() < c2.getTimeInMillis()) return 0;
        int year1 = c1.get(Calendar.YEAR);
        int year2 = c2.get(Calendar.YEAR);
        int month1 = c1.get(Calendar.MONTH);
        int month2 = c2.get(Calendar.MONTH);
        int day1 = c1.get(Calendar.DAY_OF_MONTH);
        int day2 = c2.get(Calendar.DAY_OF_MONTH);
        // 获取年的差值 假设 d1 = 2015-8-16  d2 = 2011-9-30
        int yearInterval = year1 - year2;
        // 如果 d1的 月-日 小于 d2的 月-日 那么 yearInterval-- 这样就得到了相差的年数
        if (month1 < month2 || month1 == month2 && day1 < day2) yearInterval--;
        // 获取月数差值
        int monthInterval = (month1 + 12) - month2;
        if (day1 < day2) monthInterval--;
        monthInterval %= 12;
        return yearInterval * 12 + monthInterval;
    }
}

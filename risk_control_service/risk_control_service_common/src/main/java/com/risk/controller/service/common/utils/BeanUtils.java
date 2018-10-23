package com.risk.controller.service.common.utils;

import org.apache.commons.beanutils.ConvertUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean的工具类
 *
 * @Author ZT
 * @create 2018-08-27
 */
public class BeanUtils extends org.apache.commons.beanutils.BeanUtils {
    static {
        ConvertUtils.register(new DateConvert(), java.util.Date.class);
        ConvertUtils.register(new DateConvert(), java.sql.Date.class);
        ConvertUtils.register(new DateConvert(), java.sql.Timestamp.class);
        ConvertUtils.register(new BigDecimalConvert(),java.math.BigDecimal.class);
    }

    public static void copyProperties(Object dest, Object orig)
            throws IllegalAccessException, InvocationTargetException {
        org.apache.commons.beanutils.BeanUtils.copyProperties(dest, orig);
    }

    public static Map<String, Object> beanToMap(Object entity) {
        Map<String, Object> parameter = new HashMap<String, Object>();
        Field[] fields = entity.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i].getName();
            Object o = null;
            String firstLetter = fieldName.substring(0, 1).toUpperCase();
            String getMethodName = "get" + firstLetter + fieldName.substring(1);
            Method getMethod;
            try {
                getMethod = entity.getClass().getMethod(getMethodName,
                        new Class[] {});
                o = getMethod.invoke(entity, new Object[] {});
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (o != null) {
                parameter.put(fieldName, o);
            }
        }
        return parameter;
    }

    public static Map<String, String> request2Map(Object entity) {
        Map<String, String> parameter = new HashMap<String, String>();
        Field[] fields = entity.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i].getName();
            Object o = null;
            String firstLetter = fieldName.substring(0, 1).toUpperCase();
            String getMethodName = "get" + firstLetter + fieldName.substring(1);
            Method getMethod;
            try {
                getMethod = entity.getClass().getMethod(getMethodName,
                        new Class[] {});
                o = getMethod.invoke(entity, new Object[] {});
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (o != null) {
                parameter.put(fieldName, o.toString());
            }
        }
        return parameter;
    }
}

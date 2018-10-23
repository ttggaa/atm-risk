package com.risk.controller.service.common.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Json工具类
 *
 * @Author ZT
 * @create 2018-08-27
 */
public class JacksonUtils {
    private static final Logger log = LoggerFactory.getLogger(JacksonUtils.class);
    private static ObjectMapper objectMapper = new ObjectMapper();

    public JacksonUtils() {
    }

    public static String objectToStr(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception var2) {
            log.error("object to str exception ,object:{},e:{}", object, var2);
            return null;
        }
    }

    public static Object strToObject(String str, Class<?> cls) {
        try {
            return objectMapper.readValue(str, cls);
        } catch (Exception var3) {
            log.error("object to str exception ,object:{},e:{}", str, var3);
            return null;
        }
    }

    public static <T> List<T> json2List(String json, Class<T> beanClass) {
        try {
            return (List)objectMapper.readValue(json, getCollectionType(List.class, beanClass));
        } catch (Exception var3) {
            log.error("str to list exception ,str:{},e:{}", json, var3);
            return null;
        }
    }

    public static <T> String bean2Json(T bean) {
        try {
            return objectMapper.writeValueAsString(bean);
        } catch (Exception var2) {
            log.error("object to str exception ,object:{},e:{}", bean, var2);
            return "";
        }
    }

    public static String map2Json(Map map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception var2) {
            log.error("Map to str exception ,map:{},e:{}", map, var2);
            return "";
        }
    }

    public static String list2Json(List list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception var2) {
            log.error("list to str exception ,list:{},e:{}", list, var2);
            return "";
        }
    }

    public static <T> T json2Bean(String json, Class<T> beanClass) {
        try {
            return objectMapper.readValue(json, beanClass);
        } catch (Exception var3) {
            log.error("str to object exception ,jsonStr:{},e:{}", json, var3);
            return null;
        }
    }

    public static Map json2Map(String json) {
        try {
            return (Map)objectMapper.readValue(json, Map.class);
        } catch (Exception var2) {
            log.error("jsonstr to Map exception ,jsonStr:{},e:{}", json, var2);
            return null;
        }
    }

    public static JavaType getCollectionType(Class<?> collectionClass, Class... elementClasses) {
        return objectMapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }
}

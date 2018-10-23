package com.risk.controller.service.common.utils;

import org.apache.commons.beanutils.Converter;

/**
 * @Author ZT
 * @create 2018-08-27
 */
public class BigDecimalConvert implements Converter {

    @Override
    public Object convert(Class type, Object value) {
        if(value==null){
            return null;
        }
        return value;
    }
}
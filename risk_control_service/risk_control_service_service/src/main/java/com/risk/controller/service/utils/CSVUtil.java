package com.risk.controller.service.utils;

/**
 * Created by root on 11/16/16.
 */
public class CSVUtil {
    public static boolean csvContains(String source, String seq){
        if(source == null || source.isEmpty()){
            return false;
        }

        if(seq == null || seq.isEmpty()){
            return true;
        }

        String[] arr = source.split(",");
        for(String a : arr){
            if(a.equals(seq)){
                return true;
            }
        }

        return false;
    }
}

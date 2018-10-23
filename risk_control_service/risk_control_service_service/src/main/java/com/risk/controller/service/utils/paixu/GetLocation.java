package com.risk.controller.service.utils.paixu;

import java.net.URL;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.impl.PaixuServiceImpl;

public class GetLocation {  
    public static void main(String[] args) {  
        // lat 31.2990170   纬度    
        //log 121.3466440    经度
    	
        String add = getAdd("116.5994322374132","35.40922444661458").toJSONString(); 
        System.out.println(add);
//        JSONObject jsonObject = getAdd("121.432613", "31.353474");   
//        JSONArray jsonArray = JSONArray.parseArray(jsonObject.getString("addrList")); 
//        System.out.println(jsonArray);
//        JSONObject j_2 = JSONObject.fromObject(jsonArray.get(0));  
//        String allAdd = j_2.getString("admName");  
//        String arr[] = allAdd.split(",");  
//        System.out.println("省:"+arr[0]+"\n市:"+arr[1]+"\n区:"+arr[2]);  
        DecisionHandleRequest req = new DecisionHandleRequest();
        req.setLongitude("31.2990170");
        req.setLatitude("121.432613");
        
    }  
      
    public static JSONObject getAdd(String log, String lat ){  
    	//lat 小  log  大  
        //参数解释: 纬度,经度 type 001 (100代表道路，010代表POI，001代表门址，111可以同时显示前三项) 
        String urlString = "http://gc.ditu.aliyun.com/regeocoding?l="+lat+","+log+"&type=010";  
        
//        String urlString = "http://api.map.baidu.com/geocoder/v2/?callback=renderReverse&output=json&pois=1&ak=XVWZnweW2O6wcoxF8sjW8SRuBgwpHtEy&";
        	
        String location = log + "," + lat;
        
        urlString = urlString + location;
        			
        String res = "";     
        try {     
            URL url = new URL(urlString);    
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)url.openConnection();    
            conn.setDoOutput(true);    
            conn.setRequestMethod("GET");    
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(),"UTF-8"));    
            String line;    
           while ((line = in.readLine()) != null) {    
               res += line+"\n";    
         }    
            in.close();    
        } catch (Exception e) {    
            System.out.println("error in wapaction,and e is " + e.getMessage());    
        }   
        
        return JSON.parseObject(res);    
    }  
      
}  

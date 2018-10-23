package com.risk.controller.service.utils.paixu;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * http请求参数转换
 *
 * @author xuzhen.qxz
 */
public class LocalHttpHelp {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(LocalHttpHelp.class);


    /**
     * 表单请求方式：请求参数组装
     *
     * @param map 请求参数集合
     * @param uri 请求http地址
     */
    public static HttpUriRequest formUri(Map<String, String> map, String uri) {

        List<NameValuePair> formparams = new ArrayList<>();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }

        HttpEntity reqEntity = null;
        try {
            reqEntity = new UrlEncodedFormEntity(formparams, CommonConstant.CHARSET_UTF);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }

        // 设置HttpClient请求信息
        RequestBuilder requestBuilder = RequestBuilder
                .post()
                .setCharset(Charset.forName(CommonConstant.CHARSET_UTF))
                .setEntity(reqEntity)
                .setUri(uri);
        HttpUriRequest httpUriRequest = requestBuilder.build();

        return httpUriRequest;
    }

    /**
     * json请求方式：请求参数组装
     *
     * @param json 请求参数json字符串
     * @param uri 请求http地址
     */
    public static HttpUriRequest jsonUri(String json, String uri) {

        // 设置HttpClient请求信息
        RequestBuilder requestBuilder = RequestBuilder
                .post()
                .setCharset(Charset.forName(CommonConstant.CHARSET_UTF))
                .setHeader("Content-type", CommonConstant.CONTENT_TYPE_JSON)
                .setHeader("accept", CommonConstant.CONTENT_TYPE_JSON)
                .setEntity(new StringEntity(
                        json,
                        CommonConstant.CHARSET_UTF))
                .setUri(uri);

        HttpUriRequest httpUriRequest = requestBuilder.build();

        return httpUriRequest;
    }
}

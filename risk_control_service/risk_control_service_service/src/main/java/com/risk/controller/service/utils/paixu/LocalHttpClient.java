package com.risk.controller.service.utils.paixu;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;


/**
 * 根据请求响应结果以及将响应结果转换Json
 *
 * @author xuzhen.qxz
 */
public class LocalHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(LocalHttpClient.class);

    private static int timeout = 720000;

    private static int retryExecutionCount = 5;

    private static final String MODEL_URL = "/riskcontrol/";

    protected static CloseableHttpClient httpClient = HttpClientFactory
            .createHttpClient(150, 20, timeout, retryExecutionCount);

    /**
     * @param timeout 时长
     */
    public static void setTimeout(int timeout) {
        LocalHttpClient.timeout = timeout;
    }

    /**
     * 设置参数
     *
     * @param retryExecutionCount 重连次数
     */
    public static void setRetryExecutionCount(int retryExecutionCount) {
        LocalHttpClient.retryExecutionCount = retryExecutionCount;
    }

    /**
     * 初始化
     *
     * @param maxTotal    最大连接数
     * @param maxPerRoute 路由最大连接数
     */
    public static void init(int maxTotal, int maxPerRoute) {
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        httpClient = HttpClientFactory.createHttpClient(maxTotal,
                maxPerRoute,
                timeout,
                retryExecutionCount);
    }


    /**
     * 根据 请求返回 response 响应
     *
     * @param request
     * @return
     */
    public static CloseableHttpResponse execute(HttpGet request) {
        loggerRequest(request);
        try {
            return httpClient.execute(request, HttpClientContext.create());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 根据 请求返回 response 响应
     *
     * @param request
     * @return
     */
    public static CloseableHttpResponse execute(HttpUriRequest request) {
        loggerRequest(request);
        try {
            return httpClient.execute(request, HttpClientContext.create());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 自动解析Json对象时执行的http请求 (用于 executeJsonResult 方法调用)
     *
     * @param request
     * @param responseHandler
     * @param <T>
     * @return
     */
    public static <T> T execute(HttpUriRequest request, ResponseHandler<T> responseHandler) {
        String uriId = loggerRequest(request);
        if (responseHandler instanceof LocalResponseHandler) {
            LocalResponseHandler lrh = (LocalResponseHandler) responseHandler;
            lrh.setUriId(uriId);
        }
        try {
            return httpClient.execute(request, responseHandler, HttpClientContext.create());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     *
     * @param url 地址
     * @param jsonStringParams Json参数
     * @param clazz 返回类型
     * @return
     */
    public static <T>T executePost(String url,String jsonStringParams,Class<T> clazz) {
        RequestBuilder requestBuilder = RequestBuilder
                .post()
                .setCharset(Charset.forName(CommonConstant.CHARSET_UTF))
                .setHeader("Content-Type", CommonConstant.CONTENT_TYPE_JSON)
                .setHeader("accept", CommonConstant.CONTENT_TYPE_JSON)
                .setEntity(new StringEntity(jsonStringParams,
                        CommonConstant.CHARSET_UTF))
                .setUri(url);
        HttpUriRequest request = requestBuilder.build();
        return execute(request, JsonResponseHandler.createResponseHandler(clazz));
    }

    /**
     *  post请求返回Json
     * @param url 请求地址
     * @param jsonStringParams 请求参数Str
     * @return
     */
    public static JSONObject executePostJson(String url,String jsonStringParams){
        return executePost(url,jsonStringParams,JSONObject.class);
    }

    /**
     * 数据返回自动转为Json
     *
     * @param request qo
     * @return result
     */
    public static JSONObject executeJsonResult(HttpUriRequest request) {
        return execute(request, JsonResponseHandler.createResponseHandler(JSONObject.class));
    }

    /**
     * 数据返回自动JSON对象解析
     *
     * @param request qo
     * @param clazz   clazz
     * @return result
     */
    public static <T> T executeJsonResult(HttpUriRequest request, Class<T> clazz) {
        return execute(request, JsonResponseHandler.createResponseHandler(clazz));
    }

    /**
     * 日志记录（解析时用）
     *
     * @param request qo
     * @return log qo id
     */
    private static String loggerRequest(HttpUriRequest request) {
        String id = UUID.randomUUID().toString();
        if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
            if (request instanceof HttpEntityEnclosingRequestBase) {
                HttpEntityEnclosingRequestBase request_base = (HttpEntityEnclosingRequestBase) request;
                HttpEntity entity = request_base.getEntity();
                String content = null;
                //MULTIPART_FORM_DATA 请求类型判断
                if (entity.getContentType().toString()
                        .indexOf(ContentType.MULTIPART_FORM_DATA.getMimeType()) == -1) {
                    try {
                        content = EntityUtils.toString(entity);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }

                // 模型调用参数过长，筛选去除日志
                if (request.getURI().toString().contains(MODEL_URL)) {
                    logger.info("URI[{}] {} {} ContentLength:{}",
                            id,
                            request.getURI().toString(),
                            entity.getContentType(),
                            entity.getContentLength());
                } else {
                        logger.info("URI[{}] {} {} ContentLength:{}",
                                id,
                                request.getURI().toString(),
                                entity.getContentType(),
                                entity.getContentLength()
                                );
                }
            } else {
                logger.info("URI[{}] {}", id, request.getURI().toString());
            }
        }
        return id;
    }

    /**
     * 获得服务器端数据，以InputStream形式返回
     *
     * @return
     * @throws IOException
     */
    public static InputStream getInputStream(String urlPath ) {
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(urlPath);
            if (url != null) {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                // 设置连接网络的超时时间
                httpURLConnection.setConnectTimeout(3000);
                httpURLConnection.setDoInput(true);
                // 设置本次http请求使用get方式请求
                httpURLConnection.setRequestMethod("GET");
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == 200) {
                    // 从服务器获得一个输入流
                    inputStream = httpURLConnection.getInputStream();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return inputStream;
    }

}


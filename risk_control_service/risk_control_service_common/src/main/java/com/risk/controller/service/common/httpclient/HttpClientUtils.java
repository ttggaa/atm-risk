package com.risk.controller.service.common.httpclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;

/**
 * HttpClient工具类
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Slf4j
public class HttpClientUtils {
    public static final String CHARSET = "UTF-8";
    private static ThreadLocal<Map<String, String>> httpHeader = new ThreadLocal();
    private static ThreadLocal<Map<String, Object>> httpClientConfig = new ThreadLocal();
    public static final String CONNECT_TIMEOUT = "connect_timeout";
    public static final String SOCKET_TIMEOUT = "socket_timeout";
    public static final Integer DEFAULT_CONNECT_TIMEOUT = 600000;
    public static final Integer DEFAULT_SOCKET_TIMEOUT = 600000;


    public static CloseableHttpClient buildHttpClient() {
        Map<String, Object> configSetting = new HashMap();
        if (httpClientConfig != null && null != httpClientConfig.get()) {
            configSetting = (Map)httpClientConfig.get();
        }

        RequestConfig.Builder builder = RequestConfig.custom();
        Integer connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        if (((Map)configSetting).get("connect_timeout") != null) {
            try {
                connectTimeout = Integer.valueOf(((Map)configSetting).get("connect_timeout").toString());
            } catch (Exception var6) {
                log.warn("class covert error!", var6);
                connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            }
        }

        builder.setConnectTimeout(connectTimeout);
        Integer socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        if (((Map)configSetting).get("socket_timeout") != null) {
            try {
                socketTimeout = Integer.valueOf(((Map)configSetting).get("socket_timeout").toString());
            } catch (Exception var5) {
                log.warn("class covert error!", var5);
                socketTimeout = DEFAULT_SOCKET_TIMEOUT;
            }
        }

        builder.setSocketTimeout(socketTimeout);
        RequestConfig config = builder.build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    public static String doGet(String url, Map<String, String> params) throws Exception {
        return doGet(url, params, "UTF-8");
    }

    public static String doPost(String url, Map<String, String> params) throws Throwable {
        return doPost(url, params, "UTF-8");
    }

    public static String doGet(String url, Map<String, String> params, String charset) throws Exception {
        if (StringUtils.isBlank(url)) {
            return null;
        } else {
            CloseableHttpClient httpClient = null;
            CloseableHttpResponse response = null;

            String var9;
            try {
                String result;
                if (params != null && !params.isEmpty()) {
                    List<NameValuePair> pairs = new ArrayList(params.size());
                    Iterator var6 = params.entrySet().iterator();

                    while(var6.hasNext()) {
                        Map.Entry<String, String> entry = (Map.Entry)var6.next();
                        result = (String)entry.getValue();
                        if (result != null) {
                            pairs.add(new BasicNameValuePair((String)entry.getKey(), result));
                        }
                    }

                    url = url + "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, charset));
                }

                HttpGet httpGet = new HttpGet(url);
                handlerHeader(httpGet);
                httpClient = buildHttpClient();
                response = httpClient.execute(httpGet);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    httpGet.abort();
                    throw new RuntimeException("HttpClient,error status code :" + statusCode);
                }

                HttpEntity entity = response.getEntity();
                result = null;
                if (entity != null) {
                    result = EntityUtils.toString(entity, "utf-8");
                }

                EntityUtils.consume(entity);
                response.close();
                var9 = result;
            } catch (Exception e) {
                log.error("调用接口异常url:" + url);
                log.error(e.getMessage(), e);
                throw e;
            } finally {
                if (response != null) {
                    response.close();
                }

                if (httpClient != null) {
                    httpClient.close();
                }

            }

            return var9;
        }
    }

    private static void handlerHeader(HttpRequestBase requestBase) {
        if (httpHeader != null && httpHeader.get() != null) {
            Map<String, String> map = (Map)httpHeader.get();
            Iterator var2 = map.keySet().iterator();

            while(var2.hasNext()) {
                String key = (String)var2.next();
                requestBase.addHeader(key, (String)map.get(key));
            }
        }

    }

    public static String doPost(String url, Map<String, String> params, String charset) throws Exception {
        if (StringUtils.isBlank(url)) {
            return null;
        } else {
            CloseableHttpClient httpClient = null;
            CloseableHttpResponse response = null;

            String var10;
            try {
                List<NameValuePair> pairs = null;
                if (params != null && !params.isEmpty()) {
                    pairs = new ArrayList(params.size());
                    Iterator var6 = params.entrySet().iterator();

                    while(var6.hasNext()) {
                        Map.Entry<String, String> entry = (Map.Entry)var6.next();
                        String value = (String)entry.getValue();
                        if (value != null) {
                            pairs.add(new BasicNameValuePair((String)entry.getKey(), value));
                        }
                    }
                }

                HttpPost httpPost = new HttpPost(url);
                handlerHeader(httpPost);
                if (pairs != null && pairs.size() > 0) {
                    httpPost.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));
                }

                httpClient = buildHttpClient();
                response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    httpPost.abort();
                    throw new RuntimeException("HttpClient,error status code :" + statusCode);
                }

                HttpEntity entity = response.getEntity();
                String result = null;
                if (entity != null) {
                    result = EntityUtils.toString(entity, "utf-8");
                }

                EntityUtils.consume(entity);
                var10 = result;
            } catch (Exception e) {
                log.error("调用接口异常url:" + url);
                log.error(e.getMessage(), e);
                throw e;
            } finally {
                if (response != null) {
                    response.close();
                }

                if (httpClient != null) {
                    httpClient.close();
                }

            }

            return var10;
        }
    }

    public static String doPost(String url, String jsonParam) throws Exception {
        if (StringUtils.isBlank(url)) {
            return null;
        } else {
            CloseableHttpClient httpClient = null;
            CloseableHttpResponse response = null;

            String var8;
            try {
                HttpPost httpPost = new HttpPost(url);
                handlerHeader(httpPost);
                if (StringUtils.isNotBlank(jsonParam)) {
                    StringEntity entity = new StringEntity(jsonParam);
                    entity.setContentEncoding("UTF-8");
                    entity.setContentType("application/json");
                    httpPost.setEntity(entity);
                }

                httpClient = buildHttpClient();
                response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    httpPost.abort();
                    throw new RuntimeException("HttpClient,error status code :" + statusCode);
                }

                HttpEntity entity = response.getEntity();
                String result = null;
                if (entity != null) {
                    result = EntityUtils.toString(entity, "utf-8");
                }

                EntityUtils.consume(entity);
                response.close();
                var8 = result;
            } catch (Exception e) {
                log.error("调用接口异常url:" + url);
                log.error(e.getMessage(), e);
                throw e;
            } finally {
                if (response != null) {
                    response.close();
                }

                if (httpClient != null) {
                    httpClient.close();
                }

            }

            return var8;
        }
    }

    public static String doPost(String url, String params, String contentType) throws Exception {
        if (StringUtils.isBlank(url)) {
            return null;
        } else {
            CloseableHttpClient httpClient = null;
            CloseableHttpResponse response = null;

            String var9;
            try {
                HttpPost httpPost = new HttpPost(url);
                handlerHeader(httpPost);
                if (StringUtils.isNotBlank(params)) {
                    StringEntity entity = new StringEntity(params, "UTF-8");
                    entity.setContentEncoding("UTF-8");
                    if (StringUtils.isNotBlank(contentType)) {
                        entity.setContentType(contentType);
                    }

                    httpPost.setEntity(entity);
                }

                httpClient = buildHttpClient();
                response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    httpPost.abort();
                    throw new RuntimeException("HttpClient,error status code :" + statusCode);
                }

                HttpEntity entity = response.getEntity();
                String result = null;
                if (entity != null) {
                    result = EntityUtils.toString(entity, "utf-8");
                }

                EntityUtils.consume(entity);
                response.close();
                var9 = result;
            } catch (Exception e) {
                log.error("调用接口异常url:" + url);
                log.error(e.getMessage(), e);
                throw e;
            } finally {
                if (response != null) {
                    response.close();
                }

                if (httpClient != null) {
                    httpClient.close();
                }

            }

            return var9;
        }
    }

    public static void setHeader(Map<String, String> header) {
        if (header != null) {
            httpHeader.set(header);
        }

    }

    public static void setConfig(Map<String, Object> config) {
        if (config != null) {
            httpClientConfig.set(config);
        }

    }
}

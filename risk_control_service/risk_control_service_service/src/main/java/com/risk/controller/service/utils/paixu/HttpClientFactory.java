package com.risk.controller.service.utils.paixu;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

/**
 * HttpClient工厂类
 *
 * @author xuzhen.qxz
 */
public class HttpClientFactory {

    private static final String[] supportedProtocols = new String[]{"TLSv1", "TLS"};

    private static Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);

    public static CloseableHttpClient createHttpClient() {
        return createHttpClient(100, 10, 5000, 2);
    }

    /**
     * @param maxTotal            最大连接数
     * @param maxPerRoute         每个路由最大连接数
     * @param timeout             连接超时
     * @param retryExecutionCount 重试次数
     * @return CloseableHttpClient
     */
    public static CloseableHttpClient createHttpClient(int maxTotal, int maxPerRoute,
                                                       int timeout, int retryExecutionCount) {

        try {

            ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
            SSLContext sslContext = SSLContexts.custom().useSSL().build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", plainsf)
                    .register("https", sslsf)
                    .build();
            PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
                    new PoolingHttpClientConnectionManager(registry);

            // 将最大连接数增加到100
            poolingHttpClientConnectionManager.setMaxTotal(maxTotal);
            // 将每个路由基础的连接增加到10
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxPerRoute);

            SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(timeout).build();
            poolingHttpClientConnectionManager.setDefaultSocketConfig(socketConfig);
            RequestConfig configBuilder = RequestConfig.custom()
                    // 设置连接超时
                    .setConnectTimeout(timeout)
                    // 设置读取超时
                    .setSocketTimeout(timeout)
                    // 设置从连接池获取实例的超时
                    .setConnectionRequestTimeout(timeout).build();

            return HttpClientBuilder.create()
                    .setConnectionManager(poolingHttpClientConnectionManager)
                    .setDefaultRequestConfig(configBuilder)
                    .setRetryHandler(new HttpRequestRetryHandlerImpl(retryExecutionCount))
                    .build();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return null;
    }

    /**
     * HttpClient  超时重试
     */
    private static class HttpRequestRetryHandlerImpl implements HttpRequestRetryHandler {

        private int retryExecutionCount;

        public HttpRequestRetryHandlerImpl(int retryExecutionCount) {
            this.retryExecutionCount = retryExecutionCount;
        }

        @Override
        public boolean retryRequest(
                IOException exception,
                int executionCount,
                HttpContext context) {
            // 如果已经重试了 retryExecutionCount 次，就放弃
            if (executionCount > retryExecutionCount) {
                logger.error("[已经重试了" + retryExecutionCount + " 次，放弃]" + exception.getMessage());
                return false;
            }
            // 超时
            if (exception instanceof InterruptedIOException) {
                logger.error("[超时]" + exception.getMessage());
                return false;
            }
            // 目标服务器不可达
            if (exception instanceof UnknownHostException) {
                logger.error("[目标服务器不可达]" + exception.getMessage());
                return false;
            }
            // 连接被拒绝
            if (exception instanceof ConnectTimeoutException) {
                logger.error("[连接被拒绝]" + exception.getMessage());
                return true;
            }
            // ssl握手异常
            if (exception instanceof SSLException) {
                logger.error("[ssl握手异常]" + exception.getMessage());
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            // 如果请求是幂等的，就再次尝试
            if (idempotent) {
                return true;
            }
            return false;
        }

    }
}

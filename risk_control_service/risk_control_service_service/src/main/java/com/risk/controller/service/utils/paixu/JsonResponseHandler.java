package com.risk.controller.service.utils.paixu;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Response 响应结果 Json解析 Handler
 *
 * @author xuzhen.qxz
 */
public class JsonResponseHandler {

	private static Logger logger = LoggerFactory.getLogger(JsonResponseHandler.class);

	public static <T> ResponseHandler<T> createResponseHandler(final Class<T> clazz){
		return new JsonResponseHandlerImpl<T>(null,clazz);
	}

	public static class JsonResponseHandlerImpl<T> extends LocalResponseHandler implements ResponseHandler<T> {
		
		private Class<T> clazz;
		
		public JsonResponseHandlerImpl(String uriId, Class<T> clazz) {
			this.uriId = uriId;
			this.clazz = clazz;
		}


		@Override
		public T handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			int status = response.getStatusLine().getStatusCode();

			logger.info("请求返回code：{}",status);
			if (status >= 200 && status < 300) {
				HttpEntity entity = response.getEntity();
				String str = EntityUtils.toString(entity,"utf-8");
				logger.info("URI[{}] elapsed time:{} ms RESPONSE DATA STATUS:{}",super.uriId,System.currentTimeMillis()-super.startTime,status);
				if (StringUtils.isNotEmpty(str)) {
					try {
						return JSON.parseObject(str, clazz);
					} catch (Exception e) {
						logger.error("URI[{}] 数据源返回JSON格式错误{}",super.uriId, str);
						return null;
					}
				} else {
					return null;
				}
			} else if ((status >= 400 && status < 405) || status == 413){
				logger.info("Unexpected response status: " + status);
				HttpEntity entity = response.getEntity();
				String str = EntityUtils.toString(entity,"utf-8");
				logger.info("请求失败的响应结果：{}",str);
				if (StringUtils.isNotEmpty(str)) {
					try{
						return JSON.parseObject(str, clazz);
					}catch (Exception e){
						logger.error("json解析错误 data{}",str);
						return null;
					}
				} else {
					logger.error("Unexpected response status: " + status);
					return null;
				}
			} else {
				return null;
			}
		}

		
	}
}

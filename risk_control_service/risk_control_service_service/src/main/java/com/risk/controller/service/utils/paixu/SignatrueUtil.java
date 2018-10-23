package com.risk.controller.service.utils.paixu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class SignatrueUtil {

	final static String CHAR_SET = "utf-8";

    /**
	 * 获取签名
	 * 
	 * @param lst
	 * @return
	 * @throws Exception
	 */
	public static String getSign(List<String> lst, String key) {
		try {
			String[] arr = lst.toArray(new String[0]);
			Arrays.sort(arr);
			StringBuilder query = new StringBuilder();
			for (String item : arr) {
				query.append(item);
			}
			return RsaUtils.sign(query.toString().getBytes(CHAR_SET), key);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("签名异常");
			return "";
		}
	}

	/**
	 * 获取签名
	 *
	 * @param
	 * @return
	 * @throws Exception
	 */
	public static String getSign(String str,String key) {
		try {
			return RsaUtils.sign(str.getBytes(CHAR_SET), key);
		} catch (Exception e) {
			System.out.println("签名异常");
			return "";
		}

	}

	/**
	 * 校验签名
	 *
	 * @param lst
	 * @param sign
	 * @return
	 * @throws Exception
	 */
	public static boolean vertifySign(List<String> lst, String sign ,String key) {

	    try {
            String[] arr = lst.toArray(new String[0]);
            Arrays.sort(arr);
            StringBuilder query = new StringBuilder();
            for (String item : arr) {
                query.append(item);
            }
            System.out.println(query.toString());
			return RsaUtils.verify(query.toString().getBytes("utf-8"), key, sign);
		} catch (UnsupportedEncodingException e) {
			System.out.println("校验签名异常");
			log.error("校验签名异常", e);
			return false;
		} catch (Exception e) {
			System.out.println("校验签名异常");
			log.error("校验签名异常", e);

			return false;
		}
	}

	/**
	 * 读取秘钥
	 */
	@SuppressWarnings("resource")
	public static String initKey(String keyFile) {
		try {
			Resource resource = new ClassPathResource(keyFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()));
			String s = br.readLine();
			return s;
		} catch (Exception e) {
			log.info("获取秘钥异常：",e);
			return "";
		}

	}

}

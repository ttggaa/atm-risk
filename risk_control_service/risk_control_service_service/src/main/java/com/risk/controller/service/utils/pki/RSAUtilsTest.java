package com.risk.controller.service.utils.pki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.risk.controller.service.utils.paixu.SignUtils;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * <p>
 * 由于非对称加密速度极其缓慢，一般文件不使用它来加密而是使用对称加密，<br/>
 * 非对称加密算法可以用来对对称加密的密钥加密，这样保证密钥的安全也就保证了数据的安全
 * </p>
 * <p>
 * plain:{"age":18, "name": "zhangsan"} encrypt by public
 * key:HKlpDW0lNQ0DQRUf0BuXxeT0x8VMnzRvUg5pQNEFyflkKXlGeN/
 * NcRjFs8mpaAtmZ9rO5wovl3aP9YmuPxLOQXaPMGMo2jkdF0EQIZRHtT2ihA0iyDHy7+
 * oC2rTaDvB7J2Kr+ZJ8BQVsDT+B4tppgylHs2L07Sfk5cT6n9fBqq8= encrypt by private
 * key:Snjvlc9pwAr/FeifmI5yts1wA8pvZciF3xkjVBn36VcyonSflMWtFpypLN6YN45fnL+
 * yOKn0ulOxJb9LQXhJdX1cukEgoOlMpXRZZw9a1CZPEdhDi1XsIfvezNyht2/nsNYPGa0fL55+
 * 9pYPBmwgT2XN59p2Jw3PdrUm/+Drjqg=
 *
 * </p>
 */
public class RSAUtilsTest {

	private static final Logger log = LoggerFactory.getLogger(RSAUtilsTest.class);
	public static void main(String... strings) throws Exception {
//		String file = "/etc/rsa_key_paire_pkcs8.pem";
//		String pubfile = "/etc/rsa_public_key.pem";
//
//		String signType = "SHA1WithRSA";
//		// log.debug(new String(s));
//		final PrivateKey privateKey = RSAUtils.loadPrivateKey(file);
//		log.debug(privateKey+"");
//
//		final PublicKey publicKey = RSAUtils.loadPublicKey(pubfile);
//		log.debug(publicKey+"");
//
//		String data = "{\"age\":18, \"name\": \"zhangsan\"}";
//		log.debug("-------------source:" + data);
//		String sign = RSAUtils.sign(signType, data.getBytes("utf-8"), privateKey);
//		log.debug("-------------sign:" + sign);
//		sign = "cZpL4fDdP2yzHL8bCCfn2503niJL+ApGbvXXWASFbd2nZ/J0PQxXrdMy0uUU7yq5IBAdbJM3cyG5vUnib6uJTdSAbvuv3wJm+IgSv3DNu9cMiqP3KnRIlwZ02zLe9KMl4Fb0Je+kxtnRNJZ2t7dUr5BYDmQlPkZLokV9PlvyWuI=";
//		boolean signOK = RSAUtils.verifySign(signType, data.getBytes("utf-8"), publicKey, sign);
//		log.debug("-------------signOK:" + signOK);
//		
//		long start = System.currentTimeMillis();
//		String enc = RSAUtils.encryptByPublicKey(data, publicKey, "utf-8");
//
//		log.debug("-------------enc:" + enc);
//
//		enc = "xV0hPM49N0Hpk+9EMO2DdV0GpgiCcPGEtDg7JzqWLiVK/rsz4NK4QI2Q2wrOIf+oRTyKsY2vD3trfflym7cBHxeMn1ezIlHGfOPuhYG9Rs2zj5DIJJn6BiXEBAKibEtHNwAPG28fTigHf1TuO4DI1Ivhx7yYoZtqkHdHTfKkVUQ=";
//		String back = RSAUtils.decryptByPrivateKey(enc, privateKey, "utf-8");
//		log.debug("back:" + back);
//
//		String enc2 = RSAUtils.encryptByPrivateKey(data, privateKey, "utf-8");
//		log.debug("-------------enc2:" + enc2);
//
//		String back2 = RSAUtils.decryptByPublicKey(enc2, publicKey, "utf-8");
//		log.debug("back2:" + back2);
//
//		long stop = System.currentTimeMillis();
//		log.debug("-----------done:" + (stop - start) / 1000);
		String pri = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMiOACKqEEVeIbzg3WkXPKRePfw7WjdSec6n1x52bXQEVXqoJm8gDOAZU6eT//bBKPT50hpKMGHbJOYCdvS2VZXal0pgWfUdKhLYYOtHnzxOmhQFZzwsrNZuZDWapOAXbDwutj4ljMmol1IuXEM1NSPBSROpOt8v9kQvHEyowBxTAgMBAAECgYBo5+ZoO38BWgjZzOHkO9RrwiQ4US+SqqCumZrsA1SjkDTKUTSxghlaC8V3bfJBN67d3eOp/s6qEUCO1BE3Vj0TlSBLU7dKR2NRMZBsW9oROZCA+++sQVgzSQx1PgyWgSBtCF/U4oY1pZUbXAE4pnEDcCMUrUkrRcV/aHWCGJ3HQQJBAPS37iUnyOLUUSqZdR3caFRU5PNoMCJQk/7m47FJqs2YXp13V60bjDLrf/I6Vo/y4Vvvz3Yfjmx5aDSQXNNGB6ECQQDRzN3sLuT9DPbwdgbmYHpaZXwp2cRSE6OSyo5fqSbc6WxtA3KyOBxm15jnkk0C3IU5IOyOWR8U3WpMXurd8E9zAkEA1OaP0NNj+bMtShpnwarXOUcCSKED/1aK6uCEhuDIMEW3Stdg98FXn5UyotIOLP3pAcsIeoPJrKWS+uf9WfE7AQJBAKgHxeIo+Nu3a3vRe9s9gCEwUM6QDE2UTxj9RCRXrLmX9nAlJ9KXYB/6IwutQCK/ja/gr7WeqWcjosRB7SgOFQMCQHtd2YPIFya5scW02EZW+S/xuuZeytm78f7L1Pni+0jEaAgoSb7WCU0pgC0KntBd2km/FCIcbXZ8KNvd47hQDEc=";
		String pub = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIjgAiqhBFXiG84N1pFzykXj38\n" + 
				"O1o3UnnOp9cedm10BFV6qCZvIAzgGVOnk//2wSj0+dIaSjBh2yTmAnb0tlWV2pdK\n" + 
				"YFn1HSoS2GDrR588TpoUBWc8LKzWbmQ1mqTgF2w8LrY+JYzJqJdSLlxDNTUjwUkT\n" + 
				"qTrfL/ZELxxMqMAcUwIDAQAB";
		String sss = "1234djf第三方";
		
		String sign = RSAUtils.sign("SHA1WithRSA", sss.getBytes("UTF-8"), RSAUtils.readPrivateKey(pri));
		boolean flag = RSAUtils.verifySign("SHA1WithRSA", sss.getBytes("UTF-8"), RSAUtils.readPublicKey(pub), sign);

		String sign3 = SignUtils.getSign(sss, pri);

		
//		String sign2 = RsaUtils.signData(sss, pri);
//		boolean flag2 = RsaUtils.verifyData(sss, sign2, pub);
		
		System.out.println(sign);
		System.out.println(flag);

//		System.out.println(sign2);
//
//		System.out.println(flag2);
		System.out.println(sign3);

	}

}

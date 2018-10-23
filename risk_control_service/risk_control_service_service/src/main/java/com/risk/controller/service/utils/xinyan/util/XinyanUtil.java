package com.risk.controller.service.utils.xinyan.util;

import java.util.UUID;

public class XinyanUtil {
	
	public static String generateTransId() {
		String uuid = UUID.randomUUID().toString();
		return uuid.replaceAll("-", "").toUpperCase();
	}
	
}

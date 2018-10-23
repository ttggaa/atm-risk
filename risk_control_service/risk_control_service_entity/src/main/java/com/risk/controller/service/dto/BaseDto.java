package com.risk.controller.service.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.BeanUtils;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

@Data
public class BaseDto implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -969789713464686734L;

	private Long id;
	private Long addTime;
	private Long addUser;
	private Long updateTime;
	private Long updateUser;

	public void populate (JSONObject jsonObj){
		if(null == jsonObj){
			return ;
		}
		
		try {
			BeanUtils.populate(this, jsonObj);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} catch (InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String toString(){
		return JSON.toJSONString(this);
	}
}

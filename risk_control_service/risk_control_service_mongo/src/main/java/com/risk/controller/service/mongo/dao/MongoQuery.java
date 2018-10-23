package com.risk.controller.service.mongo.dao;

import lombok.Data;

@Data
public class MongoQuery {
    private String key;
	private Object value;
	private MongoQueryBaseType baseType;
	public MongoQuery(String key, Object value, MongoQueryBaseType baseType){
		this.key = key;
		this.value = value;
		this.baseType = baseType;
	}
	public enum MongoQueryBaseType{
		ge,gt,le,lt,eq
	}
}
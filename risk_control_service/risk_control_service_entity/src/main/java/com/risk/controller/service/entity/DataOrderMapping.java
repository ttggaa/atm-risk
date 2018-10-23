package com.risk.controller.service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 * data_operator_mapping
 * @author 
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DataOrderMapping {

    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    private String phone;
    
    private String nid;
    /**
     * 运营商数据编号
     */
    private String operatorNum;
    
    /**
     * 设备数据编号
     */
    private String clientNum;

    /**
     * 添加时间
     */
    private Long addTime;
    
    private Long addUser;

    /**
     * 更新时间, unix时间戳（毫秒）,修改任何字段(除本字段外)都应更新此字段.
     */
    private Long updateTime;
    
    private Long updateUser;

}
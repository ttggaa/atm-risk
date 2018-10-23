package com.risk.controller.service.common.utils;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 响应的实体类
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Data
@ToString
public class ResponseEntity implements Serializable {
    private static final long serialVersionUID = -720807478055084231L;
    public static final String STATUS_OK = "1";
    public static final String STATUS_FAIL = "0";
    private String status;
    private String error;
    private String msg;
    private Object data;
    private Object extension;
    private Integer currentPage;
    private Integer pageCount;
    private Integer pageSize;
    private Integer recordsTotal;


    public ResponseEntity() {
    }

    public ResponseEntity(String status) {
        this.status = status;
    }

    public ResponseEntity(String status, String error) {
        this.status = status;
        this.error = error;
    }

    public ResponseEntity(String status, Object data) {
        this.status = status;
        this.data = data;
    }

    public ResponseEntity(String status, Object data, int currentPage, int pageCount, int pageSize, int recordsTotal) {
        this.status = status;
        this.data = data;
        this.currentPage = currentPage;
        this.pageCount = pageCount;
        this.pageSize = pageSize;
        this.recordsTotal = recordsTotal;
    }

    public ResponseEntity(String status, Object data, int pageCount) {
        this.status = status;
        this.data = data;
        this.pageCount = pageCount;
    }

    public ResponseEntity(String status, String error, String msg, Object data) {
        this.status = status;
        this.error = error;
        this.msg = msg;
        this.data = data;
    }
}

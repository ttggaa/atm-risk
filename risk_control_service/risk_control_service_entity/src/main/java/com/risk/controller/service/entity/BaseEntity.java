package com.risk.controller.service.entity;

import lombok.Data;


/**
 * 实体类的基本类
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Data
public class BaseEntity {
    protected Long id;// ID

    protected Long addTime;// 创建日期

    protected Long addUser;// 创建人

    protected Long updateTime;// 修改日期

    protected Long updateUser;// 修改人
}

package com.risk.controller.service.dao;

import com.risk.controller.service.entity.StaUserBaseinfo;
import org.springframework.data.repository.query.Param;

public interface StaUserBaseinfoDao {
    void saveOrUpdate(StaUserBaseinfo userBaseinfo);

    /**
     * 通过nid查询用户基础信息
     * @param nid
     * @return
     */
    StaUserBaseinfo getByNid(@Param("nid") String nid);
}
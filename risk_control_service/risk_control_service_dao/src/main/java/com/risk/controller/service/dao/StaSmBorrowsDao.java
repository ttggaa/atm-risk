package com.risk.controller.service.dao;

import com.risk.controller.service.entity.StaSmBorrows;
import org.apache.ibatis.annotations.Param;

public interface StaSmBorrowsDao {

    int saveOrUpdate(StaSmBorrows smBorrows);

    StaSmBorrows getByNid(@Param("nid") String nid);
}
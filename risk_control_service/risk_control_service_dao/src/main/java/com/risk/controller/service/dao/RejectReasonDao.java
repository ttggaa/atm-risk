package com.risk.controller.service.dao;

import org.apache.ibatis.annotations.Param;

import java.util.Set;

public interface RejectReasonDao {
    Integer getMaxCloseDays(@Param("set")Set<String> rejectReason);
}
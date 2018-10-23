package com.risk.controller.service.dao;


import com.risk.controller.service.entity.BlacklistImei;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface BlacklistImeiDao {
    int getCount(@Param("set") Set<String> set);

    List<String> getListByImeis(List<String> list);

    BlacklistImei getLastOneByImei(String string);

    void insert(BlacklistImei blacklistImei);
}
package com.risk.controller.service.dao;


import com.risk.controller.service.entity.BlacklistMac;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface BlacklistMacDao {
    int getCount(@Param("set") Set<String> set);

    List<String> getListByMacs(List<String> list);

    BlacklistMac getLastOneByMac(String string);

    void insert(BlacklistMac blacklistMac);
}
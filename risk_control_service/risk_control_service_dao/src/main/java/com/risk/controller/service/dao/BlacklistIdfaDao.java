package com.risk.controller.service.dao;


import com.risk.controller.service.entity.BlacklistIdfa;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface BlacklistIdfaDao {

    int countByset(@Param("set") Set<String> set);

    List<String> getListByIdfas(List<String> list);

    BlacklistIdfa getLastOneByIdfa(String string);

    void insert(BlacklistIdfa blacklistIdfa);
}
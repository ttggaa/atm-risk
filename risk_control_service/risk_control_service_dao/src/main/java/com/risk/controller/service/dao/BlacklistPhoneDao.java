package com.risk.controller.service.dao;


import com.risk.controller.service.entity.BlacklistPhone;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface BlacklistPhoneDao {

    /**
     * 通过手机号码查询黑名单
     * @param userName
     * @return
     */
    BlacklistPhone getLastOneByPhone(String userName);

    int countByphone(@Param("set") Set<String> set);

    void insert(BlacklistPhone blacklistPhone);

    List<String> getListByPhones(List<String> list);
}
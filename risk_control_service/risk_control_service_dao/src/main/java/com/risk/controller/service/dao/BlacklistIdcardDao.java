package com.risk.controller.service.dao;


import com.risk.controller.service.entity.BlacklistIdcard;

import java.util.List;

public interface BlacklistIdcardDao {

    BlacklistIdcard getLastOneByIdCard(String cardId);

    void insert(BlacklistIdcard blacklistIdcard);

    List<String> getListByIdcards(List<String> list);
}
package com.risk.controller.service.dao;

import com.risk.controller.service.entity.ClientContact;

import java.util.List;
import java.util.Map;

public interface ClientContactDao {
    void saveBatch(List<ClientContact> list);

    Map<String, Object> getCallNumByDay(Map<String, Object> map);

    Map<String, Object> getCallAndCalledByDay(Map<String, Object> map);

    int getRepeatPersons(Map<String, Object> map);

    Integer getValidCallDetail(Map<String, Object> param);

    Integer getAllCallDetail(Map<String, Object> param);

    Map<String, Object> getOpertorCallAndCalledNum(Map<String, Object> param);
}
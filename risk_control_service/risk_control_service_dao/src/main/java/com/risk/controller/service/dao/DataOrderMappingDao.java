package com.risk.controller.service.dao;


import com.risk.controller.service.entity.DataOrderMapping;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface DataOrderMappingDao {
    DataOrderMapping queryLastOne(DataOrderMapping data);

    DataOrderMapping queryLastOneByNid(String nid);

    List<Map<String,String>> getAllByNid(@Param("nid") String nid);
}
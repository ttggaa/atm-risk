package com.risk.controller.service.dao;

import com.risk.controller.service.entity.AdmissionResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdmissionResultDao {

    int insertSelective(AdmissionResult record);

    int updateByPrimaryKeySelective(AdmissionResult record);

    /**
     * 根据nid获取准入执行结果
     * @param nid
     * @return
     */
    AdmissionResult getLastOneByNid(@Param("nid") String nid, @Param("merchartCode") String merchartCode);

    /**
     * 查询1个小时之前挂起的数据查询30条
     * @return
     */
    List<String> queryNeedReRun();

    List<String> getAllByCondition(AdmissionResult result);
}
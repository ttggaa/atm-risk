package com.risk.controller.service.dao;

import com.risk.controller.service.entity.AdmissionResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdmissionResultDao {
    int deleteByPrimaryKey(Long id);

    int insert(AdmissionResult record);

    int insertSelective(AdmissionResult record);

    AdmissionResult selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(AdmissionResult record);

    int updateByPrimaryKey(AdmissionResult record);

    /**
     * 根据nid查询决策结果
     * @param findResult
     * @return
     */
    AdmissionResult getByNid(AdmissionResult findResult);

    /**
     * 根据resultId获取决策结果的拒绝原因编码
     * @param resultId
     * @return
     */
    List<String> getRejectReason(Long resultId);

    /**
     * 根据nid获取准入执行结果
     * @param cond
     * @return
     */
    AdmissionResult getLastOneByNid(AdmissionResult cond);


    AdmissionResult getResultLastOneByNid(@Param("nid") String nid);

    /**
     * 查询1个小时之前挂起的数据查询30条
     * @return
     */
    List<String> queryNeedReRun();

    List<String> getAllByCondition(AdmissionResult result);
}
package com.risk.controller.service.dao;

import com.risk.controller.service.entity.AdmissionResultDetail;

import java.util.List;
import java.util.Map;

public interface AdmissionResultDetailDao {
    int deleteByPrimaryKey(Long id);

    int insert(AdmissionResultDetail record);

    int insertSelective(AdmissionResultDetail record);

    AdmissionResultDetail selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(AdmissionResultDetail record);

    int updateByPrimaryKey(AdmissionResultDetail record);

    List<AdmissionResultDetail> getByResultId(AdmissionResultDetail cond);

    AdmissionResultDetail getLastDetailByResultId(String nid);

    List<Map<String, Object>> getDecisionDetail(String nid);
}
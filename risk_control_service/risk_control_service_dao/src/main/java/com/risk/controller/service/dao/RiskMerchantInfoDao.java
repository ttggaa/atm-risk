package com.risk.controller.service.dao;

import com.risk.controller.service.entity.RiskMerchantInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RiskMerchantInfoDao {

    List<RiskMerchantInfo> getAll();

    RiskMerchantInfo getOneByKey(@Param(value = "code") String code);
}
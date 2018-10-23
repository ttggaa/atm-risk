package com.risk.controller.service.dao;

import com.risk.controller.service.entity.BlacklistAreaIdcard;

import java.util.List;

public interface BlacklistAreaIdcardDao{
	int getIdcardHitCount(String idcard);
    BlacklistAreaIdcard getIdcardHit(String idCard);
}
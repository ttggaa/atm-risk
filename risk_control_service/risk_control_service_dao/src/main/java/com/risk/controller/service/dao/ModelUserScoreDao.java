package com.risk.controller.service.dao;

import com.risk.controller.service.entity.ModelUserScore;

import java.util.List;

public interface ModelUserScoreDao {
    void saveBatch(List<ModelUserScore> listSub);
}
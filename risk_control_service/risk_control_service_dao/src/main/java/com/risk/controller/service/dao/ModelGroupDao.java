package com.risk.controller.service.dao;

import com.risk.controller.service.entity.ModelGroup;

public interface ModelGroupDao {
    ModelGroup getByGroupId(Long labelGroupId);
}
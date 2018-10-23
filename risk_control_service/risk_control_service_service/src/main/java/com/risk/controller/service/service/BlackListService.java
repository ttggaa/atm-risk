package com.risk.controller.service.service;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dto.BlackListDTO;

public interface BlackListService {

	ResponseEntity queryBlacklist(BlackListDTO dto);

    ResponseEntity addBlacklist(BlackListDTO dto);
}

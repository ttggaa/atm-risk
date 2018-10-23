package com.risk.controller.service.web;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dto.BlackListDTO;
import com.risk.controller.service.service.BlackListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author ZT
 * @create 2018-09-04
 */
@RestController
@RequestMapping("api/blackList")
@Slf4j
public class BlackListController {

    @Autowired
    private BlackListService blackListService;

    @ResponseBody
    @RequestMapping(value = "/queryBlacklist", method = RequestMethod.POST)
    public ResponseEntity queryBlacklist(BlackListDTO dto) {
        return blackListService.queryBlacklist(dto);
    }

    @ResponseBody
    @RequestMapping(value = "/addBlacklist", method = RequestMethod.POST)
    public ResponseEntity addBlacklist(BlackListDTO dto) {
        return blackListService.addBlacklist(dto);
    }
}

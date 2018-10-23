package com.risk.controller.service.web;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dto.BlackListDTO;
import com.risk.controller.service.service.BlackListService;
import com.risk.controller.service.service.impl.LocalCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @Author ZT
 * @create 2018-09-04
 */
@RestController
@RequestMapping("api/config")
@Slf4j
public class ConfigController {

    @Autowired
    private LocalCache localCache;

    /**
     * 刷新缓存
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "cache/refresh", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity refresh() throws Exception {
        localCache.refresh();
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    /**
     * 查询缓存数据
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "cache/getAll", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity getAll() {
        Map<String, Map<String, String>> map = localCache.getAll();
        return new ResponseEntity(ResponseEntity.STATUS_OK, map);
    }
}

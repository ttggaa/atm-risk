package com.risk.controller.service.web;

import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.utils.xinyan.dto.XinyanRadarParamDTO;
import com.risk.controller.service.service.XinyanService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/xinyan")
@Controller
public class XinyanController {

    @Autowired
    private XinyanService xinyanService;

    /**
     * 申请雷达
     *
     * @param param  业务参数
     * @param expire 是否过期校验，1校验，0不校验
     * @return
     */
    @RequestMapping(value = "/radarApply", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity getRadarApply(XinyanRadarParamDTO param, String expire) {
        return xinyanService.getRadarApply(param, StringUtils.isBlank(expire) || "true".equals(expire));
    }

    /**
     * 行为雷达
     *
     * @param param  业务参数
     * @param expire 是否过期校验，1校验，0不校验
     * @return
     */
    @RequestMapping(value = "/radarBehavior", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity getRadarBehavior(XinyanRadarParamDTO param, String expire) {
        return xinyanService.getRadarBehavior(param, StringUtils.isBlank(expire) || "true".equals(expire));
    }

}

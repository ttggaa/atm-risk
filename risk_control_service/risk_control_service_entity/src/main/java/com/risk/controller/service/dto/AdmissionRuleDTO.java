package com.risk.controller.service.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.entity.AdmissionRule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by root on 6/2/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class AdmissionRuleDTO extends BaseDto {
    private String name;
    private String category;
    private String handler;
    private String description;
    private String rejectReasonCode;
    private Integer priority;
    private Integer enabled;

    private Long maxSuspendTimeout;
    private Integer maxSuspendCnt;
    private Integer suspendResult;

    Map<String, String> setting;

    private Long suspendTime;
    private Integer suspendCnt;
    private Integer robotAction;

    public static AdmissionRuleDTO fromAdmissionRule(AdmissionRule rule) {
        if (null == rule) {
            return null;
        }
        AdmissionRuleDTO ret = new AdmissionRuleDTO();
        ret.setId(rule.getId());
        ret.setName(rule.getName());
        ret.setCategory(rule.getCategory());
        ret.setHandler(rule.getHandler());

        ret.setDescription(rule.getDescription());
        ret.setRejectReasonCode(rule.getRejectReasonCode()); // 拒绝原因编码
        ret.setPriority(rule.getPriority());
        ret.setEnabled(rule.getEnabled());

        ret.setAddTime(rule.getAddTime());
        ret.setUpdateTime(rule.getUpdateTime());
        ret.setAddUser(rule.getAddUser());
        ret.setUpdateUser(rule.getUpdateUser());
        //挂起数据
        ret.setMaxSuspendCnt(rule.getMaxSuspendCnt());
        ret.setMaxSuspendTimeout(rule.getMaxSuspendTimeout());
        ret.setSuspendResult(rule.getSuspendResult());
        ret.setRobotAction(rule.getRobotAction()); // 评分动作: 1-执行评分, 2-跳过评分 在组与规则映射表中定义

        if (StringUtils.isNotEmpty(rule.getSetting())) {
            Map<String, String> st = new HashMap<String, String>();
            ret.setSetting(st);

            JSONObject json = JSON.parseObject(rule.getSetting());
            Set<Map.Entry<String, Object>> set = json.entrySet();
            for (Map.Entry<String, Object> item : set) {
                st.put(item.getKey(), item.getValue() == null ? null : item.getValue().toString());
            }
        }

        return ret;
    }
    /**
     *
     * @param key
     * @param valueForEmpty
     * @return
     */
    public String getSetting(String key, String valueForEmpty){
        if(null == this.setting){
            return valueForEmpty;
        }

        String val = setting.get(key);
        if(null == val || val.isEmpty()){
            return valueForEmpty;
        }

        return val;
    }

    /**
     *
     * @param key
     * @param valueForEmpty
     * @return
     */
    public int getSetting(String key, int valueForEmpty) throws NumberFormatException {
        int ret = 0;
        if(null == this.setting){
            ret = valueForEmpty;
        }else {
            String val = setting.get(key);
            if (null == val || val.isEmpty()) {
                ret = valueForEmpty;
            }else{
                ret = Integer.parseInt(val);
            }
        }

        return ret;
    }


    /**
     *
     * @param key
     * @param valueForEmpty
     * @return
     */
    public long getSetting(String key, long valueForEmpty) throws NumberFormatException {
        long ret = 0;
        if(null == this.setting){
            ret = valueForEmpty;
        }else {
            String val = setting.get(key);
            if (null == val || val.isEmpty()) {
                ret = valueForEmpty;
            }else{
                ret = Long.parseLong(val);
            }
        }

        return ret;
    }
}

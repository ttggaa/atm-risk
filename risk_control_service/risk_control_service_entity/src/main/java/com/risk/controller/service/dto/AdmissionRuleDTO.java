package com.risk.controller.service.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

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

package com.risk.controller.service.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.DateConvert;
import com.risk.controller.service.common.utils.DateTools;
import com.risk.controller.service.common.utils.IdcardUtils;
import com.risk.controller.service.common.utils.PhoneUtils;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.*;
import com.risk.controller.service.service.impl.LocalCache;
import com.risk.controller.service.util.AdmissionHandler;
import com.risk.controller.service.utils.DataBaseUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pc on 2016/6/7.
 */
@Slf4j
@Service
public class VerifyHandler implements AdmissionHandler {
    @Resource
    private LocalCache localCache;
    @Resource
    private BlacklistAreaIdcardDao blacklistAreaIdcardDao;
    @Resource
    private BlacklistPhoneDao blacklistPhoneDao;
    @Resource
    private BlacklistIdfaDao blacklistIdfaDao;
    @Resource
    private BlacklistImeiDao blacklistImeiDao;
    @Resource
    private BlacklistMacDao blacklistMacDao;
    @Resource
    private BlacklistIdcardDao blacklistIdcardDao;
    @Resource
    private ClientContactDao clientContactDao;
    @Resource
    private DataBaseUtils dataBaseUtils;
    @Resource
    private DecisionBadSmsRuleService decisionBadSmsRuleService;
    @Resource
    private XinyanService xinyanService;
    @Resource
    private WanshuService wanshuService;
    @Resource
    private MongoHandler mongoHandler;
    @Autowired
    private OperatorService operatorService;
    @Autowired
    private ThirdService thirdService;

    public AdmissionResultDTO handUp(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
        return result;
    }


    public AdmissionResultDTO approved(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        result.setResult(AdmissionResultDTO.RESULT_APPROVED);
        result.setRobotAction(AdmissionResultDTO.ROBOT_ACTION_SKIP);
        result.setData("直接通过");
        return result;
    }

    public AdmissionResultDTO rejected(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        result.setResult(AdmissionResultDTO.RESULT_REJECTED);
        result.setData("直接拒绝");
        return result;
    }

    /**
     * 决策1004，设备是否多人使用
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyDevice(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        // 同设备多人注册
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || null == rule.getSetting().get("maxCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        JSONObject rs = this.thirdService.getDeviceUsedCount(request.getUserId());
        if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("数据返回异常" + rs);
            return result;
        }
        Integer count = rs.getInteger("data");
        request.getRobotRequestDTO().setDeviceUsedCount(count);

        int maxCount = Integer.valueOf(rule.getSetting().get("maxCount"));
        if (count >= maxCount) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            result.setData(count);
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            result.setData(count);
            return result;
        }
    }

    /**
     * 1011 手机号码黑名单验证
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyPhoneBlackList(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        BlacklistPhone blacklistPhone = blacklistPhoneDao.getLastOneByPhone(request.getUserName());
        if (null != blacklistPhone && 1 == blacklistPhone.getEnable()) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            result.setData(blacklistPhone.getPhone());
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            result.setData("ok");
            return result;
        }
    }

    /**
     * 1012身份证黑名单验证
     **/
    public AdmissionResultDTO verifyIdCardBlackList(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        BlacklistIdcard idcard = blacklistIdcardDao.getLastOneByIdCard(request.getCardId());
        if (null != idcard && 1 == idcard.getEnable()) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            result.setData(idcard.getIdCard());
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            result.setData("ok");
            return result;
        }
    }

    /**
     * 1013 验证年龄小
     **/
    public AdmissionResultDTO verifyMinAge(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("minAge")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {
            int age = IdcardUtils.getAgeByIdCard(request.getCardId());
            result.setData(age);
            int minAge = Integer.valueOf(rule.getSetting().get("minAge"));
            if (age < minAge) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 1014验证年龄大
     **/
    public AdmissionResultDTO verifyMaxAge(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("maxAge")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {
            int age = IdcardUtils.getAgeByIdCard(request.getCardId());
            result.setData(age);
            int minAge = Integer.valueOf(rule.getSetting().get("maxAge"));
            if (age > minAge) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 1032 身份证地区黑名单验证
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyAreaIdCardBlackList(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        BlacklistAreaIdcard blacklistAreaIdcard = null;
        try {
            blacklistAreaIdcard = blacklistAreaIdcardDao.getIdcardHit(request.getCardId());
        } catch (Exception e) {
            log.error("查询身份证地区黑名单异常", e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }

        // 没有命中，通过
        if (null == blacklistAreaIdcard) {
            result.setData("ok");
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
        } else {
            result.setData(blacklistAreaIdcard.getCode());
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
        }
        return result;
    }

    /**
     * 决策1015：设备通话记录验证黑名单-新
     * <p>
     * 验证申请人设备通话记录是否包含黑名单号码
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifydevCallRecord(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("MinCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        List<JSONObject> list = this.mongoHandler.getUserDeviceCallRecord(request);
        if (null == list || list.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData(request.getDevicePlatform());
            return result;
        }

        try {
            Set<String> set = new HashSet<>();
            list.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    set.add(phone);
                }
            });
            if (set.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                result.setData(-1);
                return result;
            }
            int backCount = this.blacklistPhoneDao.countByphone(set);
            int minCount = Integer.valueOf(rule.getSetting().get("MinCount"));
            result.setData(backCount);

            if (backCount >= minCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 决策1016：短信验证-黑名单1.1
     * 短信类型的，没有就跳过
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifySMS(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("MinCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        List<JSONObject> list = this.mongoHandler.getUserDeviceSms(request);
        if (null == list || list.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData(-1);
            return result;
        }

        try {
            Set<String> set = new HashSet<>();
            list.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    set.add(phone);
                }
            });
            if (set.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                result.setData(-1);
                return result;
            }
            int backCount = this.blacklistPhoneDao.countByphone(set);
            int minCount = Integer.valueOf(rule.getSetting().get("MinCount"));
            result.setData(backCount);
            if (backCount >= minCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 决策1017:通讯录验证-黑名单1.1
     * 通讯录没有的，拒绝
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyContact(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("MinCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        List<JSONObject> list = this.mongoHandler.getUserDeviceContact(request);
        if (null == list || list.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
            result.setData(0);
            return result;
        }

        try {
            Set<String> set = new HashSet<>();
            list.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    set.add(phone);
                }
            });
            if (set.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                result.setData(-1);
                return result;
            }
            int backCount = this.blacklistPhoneDao.countByphone(set);
            int minCount = Integer.valueOf(rule.getSetting().get("MinCount"));
            result.setData(backCount);
            if (backCount >= minCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 决策1018：检查申请人使用设备的个数
     *
     * @param request
     * @param rule
     * @return
     **/
    public AdmissionResultDTO verifyDeviceCount(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("maxDevices")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }
        try {

            int maxDevices = Integer.valueOf(rule.getSetting().get("maxDevices"));

            JSONObject rs = this.thirdService.getDeviceCount(request.getUserId());
            if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
                result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
                result.setData("数据返回异常" + rs);
                return result;
            }
            Integer count = rs.getInteger("data");
            request.getRobotRequestDTO().setUserDeviceCount(count);
            if (count >= maxDevices) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData(count);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                result.setData(count);
                return result;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }

    }

    /**
     * 决策1019：通讯录验证-严重敏感词
     * 联系人姓名命中，次数+1
     */
    public AdmissionResultDTO verifyContactKeyWord(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("SensitiveWord")
                || !rule.getSetting().containsKey("SensitiveWordCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        List<JSONObject> list = this.mongoHandler.getUserDeviceContact(request);
        if (null == list || list.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            result.setData(0);
            return result;
        }

        try {

            // 规则黑名单名称，和命中次数
            String sensitiveWord = rule.getSetting().get("SensitiveWord");
            int sensitiveWordCount = Integer.valueOf(rule.getSetting().get("SensitiveWordCount"));
            String[] keys = sensitiveWord.split(",");
            int hitCount = 0;

            for (JSONObject json : list) {
                String contacts = json.getString("contacts");
                if (StringUtils.isNotBlank(contacts)) {
                    for (String key : keys) {
                        if (contacts.indexOf(key) >= 0) {
                            hitCount++;//命中敏感词，+1
                        }
                    }
                }
            }

            result.setData(hitCount);
            if (hitCount >= sensitiveWordCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 决策：1020短信验证-严重敏感词
     **/
    public AdmissionResultDTO verifySMSKeyWord(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("KeyWord")
                || !rule.getSetting().containsKey("KeyWordCount")
                || !rule.getSetting().containsKey("days")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        List<JSONObject> list = this.mongoHandler.getUserDeviceSms(request);
        if (null == list || list.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData(0);
            return result;
        }

        try {

            // 规则黑名单名称，和命中次数
            String KeyWord = rule.getSetting().get("KeyWord");
            int keyWordCount = Integer.valueOf(rule.getSetting().get("KeyWordCount"));
            int ruleDays = Integer.valueOf(rule.getSetting().get("days"));

            String[] keys = KeyWord.split(",");
            int hitCount = 0;
            for (JSONObject json : list) {
                String body = json.getString("body");
                String strTime = json.getString("data");// 短信时间
                // 短信有效期内才計算
                Long diffDays = Math.abs(DateTools.getDayDiff(new Date(request.getApplyTime()), DateTools.convert(strTime)));
                if (ruleDays < diffDays) {
                    continue;
                }
                if (StringUtils.isNotBlank(body)) {
                    for (String key : keys) {
                        if (body.indexOf(key) >= 0) {
                            hitCount++;//命中敏感词，+1
                            break;
                        }
                    }
                }
            }

            result.setData(hitCount);

            if (hitCount >= keyWordCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData(hitCount);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                result.setData(hitCount);
                return result;
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 决策1021短信验证-一般敏感词
     **/
    public AdmissionResultDTO verifySMSSensitiveWord(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("SensitiveWord")
                || !rule.getSetting().containsKey("SensitiveWordCount")
                || !rule.getSetting().containsKey("days")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        List<JSONObject> list = this.mongoHandler.getUserDeviceSms(request);
        if (null == list || list.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData(0);
            return result;
        }

        try {
            // 规则黑名单名称，和命中次数
            String KeyWord = rule.getSetting().get("SensitiveWord");
            int sensitiveWordCount = Integer.valueOf(rule.getSetting().get("SensitiveWordCount"));
            int ruleDays = Integer.valueOf(rule.getSetting().get("days"));
            String NotSensitiveWord = rule.getSetting().get("NotSensitiveWord"); //非敏感词
            String[] notKeys = NotSensitiveWord.split(",");
            String[] keys = KeyWord.split(",");

            int hitCount = 0;
            for (JSONObject json : list) {
                String content = json.getString("body");
                String strTime = json.getString("data");// 短信时间

                if (StringUtils.isNotBlank(content) && StringUtils.isNotBlank(strTime)) {
                    // 短信有效期内才計算
                    Long diffDays = Math.abs(DateTools.getDayDiff(new Date(request.getApplyTime()), DateTools.convert(strTime)));
                    if (ruleDays < diffDays) {
                        continue;
                    }

                    for (String key : keys) {
                        if (content.indexOf(key) >= 0) {
                            boolean hit = false;
                            // 如果短信命中敏感词，并且命中非敏感词，不计入敏感词短信条数
                            for (String notKey : notKeys) {
                                if (content.indexOf(notKey) >= 0) {
                                    hit = true;
                                    break;
                                }
                            }
                            if (!hit) {
                                hitCount++;//命中敏感词，+1
                                break;
                            }
                        }
                    }
                }
            }

            result.setData(hitCount);
            if (hitCount >= sensitiveWordCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 决策1022：短信验证-风险识别
     *
     * @param request
     * @param rule
     * @return
     **/
    public AdmissionResultDTO checkRiskSMS(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
        List<JSONObject> list = this.mongoHandler.getUserDeviceSms(request);
        if (null == list || list.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData(0);
            return result;
        }
        List<DecisionBadSmsRule> regexpList = this.decisionBadSmsRuleService.getEnabled();

        result.setResult(AdmissionResultDTO.RESULT_APPROVED);
        result.setData("ok");
        if (null != regexpList && !regexpList.isEmpty()) {

            TOP_LOOP:
            for (JSONObject json : list) {
                String content = json.getString("body");
                if (StringUtils.isBlank(content)) {
                    continue;
                }

                String smsDirection = json.getString("dataType");
                smsDirection = StringUtils.isBlank(smsDirection) ? "" : "2".equalsIgnoreCase(smsDirection) ? "in" : "out";

                for (DecisionBadSmsRule regexpObj : regexpList) {
                    String ruleDirection = regexpObj.getDirection();
                    ruleDirection = StringUtils.isBlank(ruleDirection) ? "" : ruleDirection;

                    // 检查短信方向
                    if (!(DecisionBadSmsRule.DIRECTION_ALL.equals(ruleDirection) || ruleDirection.equals(smsDirection))) {
                        continue;
                    }

                    String regexp = regexpObj.getRuleRegexp();

                    if (content.matches(regexp)) { // 命中
                        this.insertBlacklist(request.getUserName(), request.getCardId());
                        result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                        result.setData(regexpObj.getId());
                        break TOP_LOOP;//命中即结束
                    }
                }
            }
        }

        return result;
    }

    /**
     * 插入黑名单
     *
     * @param phone  手机号码
     * @param cardId 身份证
     */
    private void insertBlacklist(String phone, String cardId) {
        try {
            if (StringUtils.isNotBlank(phone)) {
                BlacklistPhone blacklistPhone = blacklistPhoneDao.getLastOneByPhone(phone);
                if (null == blacklistPhone) {
                    blacklistPhone = new BlacklistPhone();
                    blacklistPhone.setPhone(phone);
                    blacklistPhone.setEnable(1);
                    blacklistPhone.setSource("risk");
                    blacklistPhone.setRemark("");
                    blacklistPhone.setAddTime(System.currentTimeMillis());
                    blacklistPhone.setUpdateTime(System.currentTimeMillis());
                    blacklistPhoneDao.insert(blacklistPhone);
                }
            }
            if (StringUtils.isNotBlank(cardId)) {
                BlacklistIdcard blacklistIdcard = blacklistIdcardDao.getLastOneByIdCard(cardId);
                if (null == blacklistIdcard) {
                    blacklistIdcard = new BlacklistIdcard();
                    blacklistIdcard.setIdCard(phone);
                    blacklistIdcard.setEnable(1);
                    blacklistIdcard.setSource("risk");
                    blacklistIdcard.setRemark("");
                    blacklistIdcard.setAddTime(System.currentTimeMillis());
                    blacklistIdcard.setUpdateTime(System.currentTimeMillis());
                    blacklistIdcardDao.insert(blacklistIdcard);
                }
            }
        } catch (Exception e) {
            log.error("插入黑名单失败", e);
        }
    }

    /**
     * 决策1023验证通讯录注册人数
     **/
    public AdmissionResultDTO verifyContactIsRegister(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("warningCount")
                || !rule.getSetting().containsKey("dangerCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        List<JSONObject> list = this.mongoHandler.getUserDeviceContact(request);
        if (null == list || list.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
            result.setData(0);
            return result;
        }

        try {

            Set<String> set = new HashSet<>();
            for (JSONObject json : list) {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    set.add(phone);
                }
            }
            JSONObject rs = this.thirdService.getRegisterCount(set);
            if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
                result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
                result.setData("数据返回异常" + rs);
                return result;
            }
            int warningCount = Integer.valueOf(rule.getSetting().get("warningCount"));
            int dangerCount = Integer.valueOf(rule.getSetting().get("dangerCount"));
            int count = rs.getInteger("data");
            result.setData(count);
            request.getRobotRequestDTO().setUserDeviceContactRegisterCount(count);

            //危险阶段
            if (count >= dangerCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }
            if (count >= warningCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 决策1024，通讯录中联系人数量验证
     **/
    public AdmissionResultDTO verifyContactNum(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("minCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {
            // 调用数仓查询通讯录
            List<JSONObject> deviceContact = this.mongoHandler.getUserDeviceContact(request);

            // 手机号码去重，并验证是否11位
            Set<String> set = new HashSet<>();
            if (null != deviceContact || deviceContact.size() > 0) {
                for (JSONObject contact : deviceContact) {
                    String phone = contact.getString("contactsPhone");
                    phone = PhoneUtils.cleanTel(phone);
                    if (PhoneUtils.isMobile(phone)) {
                        set.add(phone);
                    }
                }
            }
            result.setData(set.size());
            request.getRobotRequestDTO().setUserDeviceContacCount(set.size());
            int minCount = Integer.valueOf(rule.getSetting().get("minCount"));
            if (set.size() < minCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }


    /**
     * 决策1037：通讯录、聚信立、通话记录短号验证
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyShortNoAll(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("minCount")
                || !rule.getSetting().containsKey("shortNos")
                || !rule.getSetting().containsKey("count110")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {

            List<JSONObject> operatorCallDetail = this.mongoHandler.getUserOperatorCallDetail(request);
            if (null == operatorCallDetail || operatorCallDetail.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到运营商通话记录");
                return result;
            }
            // 保存基础数据
            operatorService.saveAllOperator(request.getNid());

            List<JSONObject> contacts = this.mongoHandler.getUserDeviceContact(request);
            List<JSONObject> callRecords = this.mongoHandler.getUserDeviceCallRecord(request);

            Set<String> phones = new HashSet<>(); // 存放所有短号号码，
            if (null != contacts && contacts.size() > 0) {
                for (JSONObject json : contacts) {
                    String phone = json.getString("contactsPhone");
                    phone = PhoneUtils.cleanTel(phone);
                    if (StringUtils.isNotBlank(phone) && phone.length() == 3) {
                        phones.add(phone);
                    }
                }
            }

            if (null != callRecords && callRecords.size() > 0) {
                for (JSONObject json : callRecords) {
                    String phone = json.getString("contactsPhone");
                    phone = PhoneUtils.cleanTel(phone);
                    if (StringUtils.isNotBlank(phone) && phone.length() == 3) {
                        phones.add(phone);
                    }
                }
            }

            Integer count110 = 0;

            for (JSONObject jsonObject : operatorCallDetail) {
                String peerNumber = jsonObject.getString("peer_number");//通话手机号码
                if ("110".equals(peerNumber)) {
                    count110++;
                }
                if (StringUtils.isNotBlank(peerNumber) && peerNumber.length() == 3) {
                    phones.add(peerNumber);
                }
            }

            // 命中规则
            int minCount = Integer.valueOf(rule.getSetting().get("minCount"));
            String shortNos = rule.getSetting().get("shortNos");
            String[] shorts = shortNos.split(",");
            Integer ruleCount110 = Integer.valueOf(rule.getSetting().get("count110"));


            if (count110 >= ruleCount110) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData(110 + ":" + count110);
                return result;
            }

            phones.removeAll(Arrays.asList(shorts));
            int count = phones.size();
            result.setData(count);
            request.getRobotRequestDTO().setUserShortNumCount(count);
            if (count >= minCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error("1037异常,data:{},error", JSONObject.toJSONString(request), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 1033 紧急联系人验证
     * 判断紧急联系人手机号是否是注册手机号
     */
    public AdmissionResultDTO verifyEmergencyContact(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        try {
            List<JSONObject> list = this.mongoHandler.getUserMainContact(request);
            if (null == list || list.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("未查询到紧急联系人");
                return result;
            }

            Set<String> set = new HashSet<>();
            list.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    set.add(phone);
                }
            });

            if (set.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("紧急联系人电话不合法");
                return result;
            }

            if (list.size() != set.size()) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("紧急联系人电话重复");
                return result;
            }

            if (set.contains(request.getUserName())) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("紧急联系人电话与注册号码一致");
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                result.setData("ok");
                return result;
            }


        } catch (Exception e) {
            log.error("决策1033异常", e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }


    /**
     * 决策1026 手机号码实名验证
     **/
    public AdmissionResultDTO verifyJuXinLiIsRealName(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
//        JSONObject operatorReport = this.getUserOperatorReport(request);
        JSONObject operatorReport = this.mongoHandler.getOperatorInfo(request);

        if (null == operatorReport) {
            result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
            result.setData("未查询到运营商通话记录");
            return result;
        }

        // 本机实名状态 -1未知　0未实名 1已实名
        Integer reliability = null == operatorReport ? 0 : operatorReport.getInteger("reliability");
        reliability = null == reliability ? 0 : reliability;
        result.setData(reliability);

        String name = operatorReport.getString("name");
        String idcard = operatorReport.getString("idcard");

        if (1 == reliability) {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        } else if (-1 == reliability) {
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(idcard)) {
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_MANUAL);
                return result;
            }
        } else {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        }
    }


    /**
     * 决策1031 手机号码使用时间验证
     */
    public AdmissionResultDTO verifyMobildIsNew(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule || null == rule.getSetting() || !rule.getSetting().containsKey("minCount")) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {

            JSONObject operatorReport = this.mongoHandler.getOperatorInfo(request);
            if (null == operatorReport || !operatorReport.containsKey("open_time")) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到手机入网时间");
                return result;
            }

            int minCount = Integer.valueOf(rule.getSetting().get("minCount")); //决策最少活跃时间
            String openTimeStr = operatorReport.getString("open_time");
            int monthCount = 0;
            if (StringUtils.isBlank(openTimeStr)) {
                JSONArray array = operatorReport.getJSONArray("bills");
                monthCount = null == array ? 0 : array.size();
                if (monthCount < minCount) {
                    JSONObject month_info = operatorReport.getJSONObject("month_info");
                    if (null != month_info) {
                        monthCount = month_info.getInteger("month_count");
                    }
                }
            } else {
                Date openTime = DateTools.convert(openTimeStr);
                monthCount = DateConvert.getMonthDiff(new Date(), openTime);
            }
            request.getRobotRequestDTO().setUserOpertorPhoneUsedTime(monthCount);
            result.setData(monthCount);

            if (monthCount >= minCount) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }
        } catch (Exception e) {
            log.error("决策：1031手机号码使用时间验证异常，nid:{},e", request.getNid(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 决策：1027 设备有效通讯录黑名单验证
     **/
    public AdmissionResultDTO verifyjxlCallRecord(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || rule.getSetting().size() <= 0
                || !rule.getSetting().containsKey("MinCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {
            // 查询设备通讯录
            List<JSONObject> deviceContactList = this.mongoHandler.getUserDeviceContact(request);
            if (null == deviceContactList || deviceContactList.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData(0);
                return result;
            }

            Set<String> devicePhones = new HashSet<>();
            deviceContactList.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    devicePhones.add(phone);
                }
            });

            List<JSONObject> operatorCallDetail = this.mongoHandler.getUserOperatorCallDetail(request);
            if (null == operatorCallDetail || operatorCallDetail.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到运营商通话记录");
                return result;
            }

            Set<String> phones = new HashSet<>(); // 存放有效通讯录
            for (JSONObject json : operatorCallDetail) {
                String phone = json.getString("peer_number");
                Long duration = json.getLong("duration");//通话时长
                if (StringUtils.isNotBlank(phone) && null != duration && duration > 0 && devicePhones.contains(phone)) {
                    phones.add(phone);
                }
            }

            // 如果有效通讯录为null，挂起
            if (phones.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData(0);
                return result;
            }

            int count = blacklistPhoneDao.countByphone(phones);
            int minCount = Integer.valueOf(rule.getSetting().get("MinCount"));

            result.setData(count);

            if (count >= minCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error("决策：1027 设备有效通讯录黑名单验证异常,nid:{},e:{}", request.getNid(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 1034紧急联系人黑名单验证
     */
    public AdmissionResultDTO verifyContactBlackList(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
        try {
            List<JSONObject> list = this.mongoHandler.getUserMainContact(request);
            if (null == list || list.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到紧急联系人");
                return result;
            }

            Set<String> set = new HashSet<>();
            list.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    set.add(phone);
                }
            });

            if (set.size() < 2) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("紧急联系人必须为2个");
                return result;
            }

            if (set.size() > 0) {
                int count = blacklistPhoneDao.countByphone(set);
                if (count > 0) {
                    result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                    result.setData("紧急联系人命中黑名单");
                    return result;
                }
            }
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            result.setData("ok");
            return result;

        } catch (Exception e) {
            log.error("决策1034异常", e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 决策：1038
     * idfa黑名单验证
     */
    public AdmissionResultDTO verifyIdfaBlackList(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        JSONObject rs = this.getUserDeviceInfo(request);
        if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("数据返回异常" + rs);
            return result;
        }
        JSONObject data = rs.getJSONObject("data");
        JSONArray idfa = data.getJSONArray("idfa");
        if (null == idfa || idfa.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            result.setData(-1);
            return result;
        }
        Set<String> set = new HashSet<>();
        idfa.forEach(o -> {
            String str = String.valueOf(o);
            if (StringUtils.isNotBlank(str)) {
                set.add(str.trim());
            }
        });

        int count = 0;
        if (set.size() > 0) {
            count = blacklistIdfaDao.countByset(set);
        }
        result.setData(count);
        if (count > 0) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        }
    }

    /**
     * 获取用户设备信息
     *
     * @param request
     * @return
     */
    private JSONObject getUserDeviceInfo(DecisionHandleRequest request) {
        if (null != request) {
            // 查询缓存
            if (null != request.getRobotRequestDTO().getUserDeviceInfo()) {
                return request.getRobotRequestDTO().getUserDeviceInfo();
            }
            if (null != request.getUserId() && 0L < request.getUserId()) {
                String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "atm.userInfo.url");
                Map<String, String> params = new HashMap<>();
                params.put("userId", String.valueOf(request.getUserId()));
                try {
                    String resultStr = HttpClientUtils.doPost(url, JSONObject.toJSONString(params), "application/json");
                    JSONObject json = JSONObject.parseObject(resultStr);
                    if (null != json && null != json.get("data") && "0".equals(json.getString("code"))) {
                        request.getRobotRequestDTO().setUserDeviceInfo(json);
                    }
                    return json;
                } catch (Throwable e) {
                    log.error("查询用户的设备被多少个用户使用异常，userId:{},e:{}", request.getUserId(), e);
                }
            }
        }
        return null;
    }


    /**
     * 决策1039,黑名单-mac
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyMacBlackList(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        JSONObject rs = this.getUserDeviceInfo(request);
        if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("数据返回异常" + rs);
            return result;
        }
        JSONObject data = rs.getJSONObject("data");
        JSONArray mac = data.getJSONArray("mac");
        if (null == mac || mac.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            result.setData(-1);
            return result;
        }


        Set<String> set = new HashSet<>();
        mac.forEach(o -> {
            String str = String.valueOf(o);
            if (StringUtils.isNotBlank(str)) {
                set.add(str.trim());
            }
        });

        int count = 0;
        if (set.size() > 0) {
            count = blacklistMacDao.getCount(set);
        }

        result.setData(count);
        if (count > 0) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        }
    }


    /**
     * 决策1040，黑名单-imei
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyImeiBlackList(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        JSONObject rs = this.getUserDeviceInfo(request);
        if (null == rs || null == rs.get("data") || !"0".equals(rs.getString("code"))) {
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("数据返回异常" + rs);
            return result;
        }
        JSONObject data = rs.getJSONObject("data");
        JSONArray imei = data.getJSONArray("imei");
        if (null == imei || imei.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            result.setData(-1);
            return result;
        }
        Set<String> set = new HashSet<>();

        imei.forEach(o -> {
            String str = String.valueOf(o);
            if (StringUtils.isNotBlank(str)) {
                set.add(str.trim());
            }
        });

        int count = 0;
        if (set.size() > 0) {
            count = blacklistImeiDao.getCount(set);
        }


        result.setData(count);
        if (count > 0) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        }
    }

    /**
     * 1035紧急联系人通话次数
     */
    public AdmissionResultDTO verifyMainContactCallCount(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule || rule.getSetting() == null
                || !rule.getSetting().containsKey("callCount")
                || !rule.getSetting().containsKey("callTime")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            return result;
        }

        try {
            List<JSONObject> list = this.mongoHandler.getUserMainContact(request);
            if (null == list || list.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("未查询到紧急联系人");
                return result;
            }

            Set<String> phoneSet = new HashSet<>();
            list.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    phoneSet.add(phone);
                }
            });

            if (phoneSet.size() < 2) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("紧急联系人至少2个");
                return result;
            }

            List<JSONObject> operatorCallDetail = this.mongoHandler.getUserOperatorCallDetail(request);
            if (null == operatorCallDetail || operatorCallDetail.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到运营商通话记录");
                return result;
            }

            int ruleCallCount = Integer.valueOf(rule.getSetting().get("callCount")); // 通话次数
            int ruleCallTime = Integer.valueOf(rule.getSetting().get("callTime")); // 通话次数

            int callCount = 0;
            int callTime = 0;

            for (JSONObject jsonObject : operatorCallDetail) {
                String peerNumber = jsonObject.getString("peer_number");//通话手机号码
                Integer duration = jsonObject.getInteger("duration");//通话时长
                if (PhoneUtils.isMobile(peerNumber)
                        && null != duration && duration > 0
                        && peerNumber.contains(peerNumber)) {

                    callCount++;
                    callTime += duration;
                }
            }

            result.setData(callCount);
            if (callCount >= ruleCallCount) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }

            result.setData(callTime);
            if (callTime >= ruleCallTime) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }

            // 其他情况拒绝
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        } catch (Exception e) {
            log.error("决策：1035紧急联系人通话次数异常，nid:{},error:{}", request.getNid(), e);
            result.setData("程序异常");
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 1036紧急联系人通话时长
     */
    public AdmissionResultDTO verifyMainContactCallTime(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule || rule.getSetting() == null
                || !rule.getSetting().containsKey("callTime")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            return result;
        }

        try {
            List<JSONObject> list = this.mongoHandler.getUserMainContact(request);
            if (null == list || list.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("未查询到紧急联系人");
                return result;
            }

            Set<String> phoneSet = new HashSet<>();       // 存放配偶的手机号码
            list.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    phoneSet.add(phone);
                }
            });

            if (phoneSet.size() < 2) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("紧急联系人至少2个");
                return result;
            }

            List<JSONObject> operatorCallDetail = this.mongoHandler.getUserOperatorCallDetail(request);
            if (null == operatorCallDetail || operatorCallDetail.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到运营商通话记录");
                return result;
            }


            int callCount = 0;
            int callTime = 0;
            for (JSONObject jsonObject : operatorCallDetail) {
                String peerNumber = jsonObject.getString("peer_number");//通话手机号码
                Integer duration = jsonObject.getInteger("duration");//通话时长

                if (PhoneUtils.isMobile(peerNumber)
                        && null != duration && duration > 0
                        && peerNumber.contains(peerNumber)) {

                    callCount++;
                    callTime += duration;
                }
            }

            int ruelCallTime = Integer.valueOf(rule.getSetting().get("callTime")); // 通话时间
            if (callTime >= ruelCallTime) {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        } catch (Exception e) {
            log.error("决策：1036紧急联系人通话时长异常，nid:{},error:{}", request.getNid(), e);
            result.setData("程序异常");
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 获取联系人的通话时长
     *
     * @param set   联系人
     * @param calls 运营商数据
     * @return Map
     * map.put("callCount", callCount);
     * map.put("callTime", callTime);
     */
    private Map<String, Integer> getContactCountAndTime(Set<String> set, JSONArray calls) {
        int callCount = 0;
        int callTime = 0;

        if (null != calls && calls.size() > 0) {
            for (Object o : calls) {
                JSONObject contact = (JSONObject) o;
                if (set.contains(contact.getString("peerNumber"))) {
                    callCount += contact.getInteger("callCount");
                    Double d = contact.getDouble("callTime");
                    callTime += d.intValue();
                }
            }
        }
        Map<String, Integer> map = new HashedMap();
        map.put("callCount", callCount); // 通过次数
        map.put("callTime", callTime); // 通话时长
        return map;
    }

    /**
     * 字符串集合中元素前两个字的重合比例计算并比对
     *
     * @param standard 指定比例
     * @param strs     字符串集合
     **/
    private Object checkStr(List<String> strs, double standard) {
        Map<String, Integer> tmpmap = new HashMap<String, Integer>();
        for (String str : strs) {
            tmpmap.put(str.length() > 2 ? str.substring(0, 2) : str, 0);
        }
        //处理数据
        for (String key : tmpmap.keySet()) {
            if (StringUtils.isNumeric(key) || StringUtils.isEmpty(key)) {
                continue;
            }
            int count = 0;
            for (String str : strs) {
                if (str.indexOf(key) > -1) {
                    count++;
                }
            }
            tmpmap.put(key, count);
        }
        LinkedList<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(tmpmap.entrySet());
        log.warn(JSON.toJSONString(list));
        //排序
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });
        double maxCount = list.get(0).getValue();
        if ((maxCount / strs.size()) >= standard) {
            return list.get(0);
        }
        return null;
    }

    /**
     * 判断字符串中是否包含特殊符号
     *
     * @Param str:被判断的字符串
     */
    private boolean hasSymbol(String str) {
        String regEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }


    /**
     * 1046平均话费>=45,且小于400通过，拉取数据月份小于2的拒绝
     * {"avgCharge":"2000"}
     */
    public AdmissionResultDTO verifyUserCharge(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule || rule.getSetting() == null
                || !rule.getSetting().containsKey("avgCharge")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            return result;
        }

        BigDecimal ruleAvgCharge = new BigDecimal(rule.getSetting().get("avgCharge"));
//        int ruleMaxCharge = Integer.valueOf(rule.getSetting().get("maxCharge"));


        JSONObject opertorInfo = this.mongoHandler.getOperatorInfo(request);

        if (null == opertorInfo || null == opertorInfo.get("bills")) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("bills");
            return result;
        }

        BigDecimal averageFare = null == opertorInfo.get("averageFare") ? BigDecimal.ZERO : opertorInfo.getBigDecimal("averageFare");
        averageFare = averageFare.multiply(new BigDecimal(100));
        result.setData(averageFare);
        request.getRobotRequestDTO().setUserOperatorAvgCharge(averageFare.intValue());

        if (averageFare.compareTo(ruleAvgCharge) < 0) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        }

        // 其他情况拒绝
        result.setResult(AdmissionResultDTO.RESULT_APPROVED);
        return result;
    }

    /**
     * 验证手机连号
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyPhoneContinuous(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule || rule.getSetting() == null
                || !rule.getSetting().containsKey("length")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            return result;
        }

        // 验证手机号码是否11位
        if (request.getUserName().length() != 11) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        }

        int length = Integer.valueOf(rule.getSetting().get("length"));

        int continuousLength = PhoneUtils.checkPhoneContinuous(request.getUserName());
        result.setData(continuousLength);

        // 连号大于规则长度拒绝
        if (continuousLength >= length) {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        } else {
            // 其他情况拒绝
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        }
    }

    /**
     * 验证用户民族是否命中规则
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyUserNation(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule || rule.getSetting() == null
                || !rule.getSetting().containsKey("keys")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            return result;
        }

        // 未查询到民族，跳过
        if (StringUtils.isBlank(request.getUserNation())) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("民族为空");
            return result;
        }

        result.setData(request.getUserNation());
        String[] keys = rule.getSetting().get("keys").split(",");
        for (String key : keys) {
            if (request.getUserNation().indexOf(key) >= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }
        }
        result.setResult(AdmissionResultDTO.RESULT_APPROVED);
        return result;
    }


    /**
     * 1050 通讯录与运营商通通话次数校验
     */
    public AdmissionResultDTO verifyOpertorCount(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule || rule.getSetting() == null
                || !rule.getSetting().containsKey("android")
                || !rule.getSetting().containsKey("ios")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            return result;
        }

        try {
            List<JSONObject> deviceContact = this.mongoHandler.getUserDeviceContact(request);
            if (null == deviceContact || deviceContact.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("设备通讯录为空");
                return result;
            }

            List<JSONObject> operatorCallDetail = this.mongoHandler.getUserOperatorCallDetail(request);
            if (null == operatorCallDetail || operatorCallDetail.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到运营商通话记录");
                return result;
            }

            // 设备通讯录去重
            Set<String> set = new HashSet<>();
            for (JSONObject contact : deviceContact) {
                String phone = contact.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    set.add(phone);
                }
            }
            if (set.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("设备通讯录数量过少");
                return result;
            }

            int count = 0;

            for (JSONObject jsonObject : operatorCallDetail) {
                String peerNumber = jsonObject.getString("peer_number");//通话手机号码
                Long duration = jsonObject.getLong("duration");//通话时长
                if (PhoneUtils.isMobile(peerNumber) && null != duration && duration > 0 && set.contains(peerNumber)) {
                    count++;
                }
            }

            Integer ruleAndroidCount = Integer.valueOf(rule.getSetting().get("android"));
            Integer ruleIosCount = Integer.valueOf(rule.getSetting().get("ios"));

            result.setData(count);

            if (DecisionHandleRequest.DEVICE_IOS.equals(request.getDevicePlatform())) {
                if (count >= ruleIosCount) {
                    result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                    return result;
                }
            }

            if (DecisionHandleRequest.DEVICE_ANDROID.equals(request.getDevicePlatform())) {
                if (count >= ruleAndroidCount) {
                    result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                    return result;
                }
            }

            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        } catch (Exception e) {
            log.error("1050 通讯录与运营商通通话次数校验异常，request;{},error", request, e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("程序异常");
            return result;
        }
    }

    /**
     * 1052 用户、紧急联系人手机号码空号、羊毛党验证
     */
    public AdmissionResultDTO verifyPhone(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        try {

            List<JSONObject> list = this.mongoHandler.getUserMainContact(request);
            if (null == list || list.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("未查询到紧急联系人");
                return result;
            }

            /**********************用户本身验证*********************/
            // 空号
            boolean bool = this.checkUserPhoneAndMainContact(request.getNid(), request.getUserName(), result);
            if (!bool) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData(request.getUserName());
                return result;
            }


            Set<String> phoneSet = new HashSet<>();
            list.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    phoneSet.add(phone);
                }
            });

            int count = 0;
            List<String> phoneList = new ArrayList<>();
            /**********************紧急联系人验证*********************/
            for (String phone : phoneSet) {
                bool = this.checkUserPhoneAndMainContact(request.getNid(), phone, result);
                if (!bool) {
                    phoneList.add(phone);
                    count++;
                }
            }
            request.getRobotRequestDTO().setRobotKhYmdCount(phoneList.size());

            result.setData(StringUtils.join(phoneList, ","));
            if (count >= 2) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }

            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        } catch (Exception e) {
            log.error("1052用户、紧急联系人手机号码空号、羊毛党验证异常，request;{},error", request, e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("程序异常");
            return result;
        }
    }

    private boolean checkUserPhoneAndMainContact(String nid, String phone, AdmissionResultDTO result) {
        WanshuReqLog wanshuReqLog = wanshuService.queryKonghao(nid, phone);
        if (null != wanshuReqLog) {
            if (StringUtils.isNotBlank(wanshuReqLog.getStatus()) && !"1".equals(wanshuReqLog.getStatus())) {
                return false;
            }
        }

        // 羊毛党
        wanshuReqLog = wanshuService.yangmaodang(nid, phone);
        if (null != wanshuReqLog) {
            result.setData(wanshuReqLog.getStatus());
            if (StringUtils.isNotBlank(wanshuReqLog.getStatus()) && !"W1".equals(wanshuReqLog.getStatus())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 1053 用户设备通讯录姓名是 3-6位数字的拒掉
     */
    public AdmissionResultDTO verifyUserContactName(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule || rule.getSetting() == null
                || !rule.getSetting().containsKey("nameKey")
                || !rule.getSetting().containsKey("maxCount")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            return result;
        }
        try {

            List<JSONObject> list = this.mongoHandler.getUserDeviceContact(request);
            if (null == list || list.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("未查询到设备通讯录");
                return result;
            }

            String nameKey = rule.getSetting().get("nameKey");
            Integer maxCount = Integer.valueOf(rule.getSetting().get("maxCount"));

            Pattern pattern = Pattern.compile(nameKey);
            int count = 0;
            for (JSONObject json : list) {
                String name = json.getString("contacts");
                if (StringUtils.isBlank(name)) {
                    continue;
                }
                Matcher isNum = pattern.matcher(name);
                if (isNum.matches()) {
                    count++;
                }
            }
            result.setData(count);
            if (count >= maxCount) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error("1053 用户设备通讯录姓名是 3-6位数字异常，request;{},error", request, e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("程序异常");
            return result;
        }
    }

    /**
     * 1055：30天内，有效通话人数，次数
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyOperatorCallNumAndPeopleNum(DecisionHandleRequest request, AdmissionRuleDTO rule) {

        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || null == rule.getSetting()
                || !rule.getSetting().containsKey("days")
                || !rule.getSetting().containsKey("callNum")
                || !rule.getSetting().containsKey("peopleNum")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        // 查询设备通讯录
        List<JSONObject> deviceContactList = this.mongoHandler.getUserDeviceContact(request);
        if (null == deviceContactList || deviceContactList.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
            result.setData(0);
            return result;
        }

        Set<String> devicePhones = new HashSet<>();
        deviceContactList.forEach(json -> {
            String phone = json.getString("contactsPhone");
            phone = PhoneUtils.cleanTel(phone);
            if (PhoneUtils.isMobile(phone)) {
                devicePhones.add(phone);
            }
        });

        Long ruleDays = Long.valueOf(rule.getSetting().get("days"));
        Long ruleCallNum = Long.valueOf(rule.getSetting().get("callNum"));
        Long rulePeopleNum = Long.valueOf(rule.getSetting().get("peopleNum"));

        List<JSONObject> operatorCallDetail = this.mongoHandler.getUserOperatorCallDetail(request);
        if (null == operatorCallDetail || operatorCallDetail.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
            result.setData("未查询到运营商通话记录");
            return result;
        }

        Map<String, Integer> resultMap = new HashMap<>();
        for (JSONObject jsonObject : operatorCallDetail) {
            String strTime = jsonObject.getString("time");// 通话时间
            String peerNumber = jsonObject.getString("peer_number");//通话手机号码
            Long duration = jsonObject.getLong("duration");//通话时长

            if (!PhoneUtils.isMobile(peerNumber) || StringUtils.isBlank(strTime) || null == duration || duration == 0) {
                continue;
            }

            Long diffDays = Math.abs(DateTools.getDayDiff(new Date(request.getApplyTime()), DateTools.convert(strTime)));

            if (diffDays <= ruleDays && devicePhones.contains(peerNumber)) {
                if (resultMap.containsKey(peerNumber)) {
                    resultMap.put(peerNumber, resultMap.get(peerNumber) + 1);
                } else {
                    resultMap.put(peerNumber, 1);
                }
            }
        }
        int callNum = 0;
        for (Map.Entry<String, Integer> entry : resultMap.entrySet()) {
            if (entry.getValue() >= ruleCallNum) {
                callNum++;
            }
        }
        if (callNum >= rulePeopleNum) {
            result.setData(callNum);
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        } else {
            result.setData(callNum);
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        }
    }

    /**
     * 1058 7天内，0次互相通话拒绝
     */
    public AdmissionResultDTO verifyCallsEach(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || !rule.getSetting().containsKey("callNum")
                || !rule.getSetting().containsKey("callDay")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {
            // 获取天数通话限制
            Integer ruleNum = Integer.valueOf(rule.getSetting().get("callNum"));
            Integer ruleDay = Integer.valueOf(rule.getSetting().get("callDay"));
            Integer userCalledNum = operatorService.robotCallAndCalledNum7(request.getNid(), request.getApplyTime(), ruleDay);
            result.setData(userCalledNum);

            // 校验
            if (ruleNum.compareTo(userCalledNum) >= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;

        } catch (Exception e) {
            log.error("[决策校验-互相通话校验异常]：单号：{}", request.getNid(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("生成并获取互通记录异常");
            return result;
        }
    }

    /**
     * 1059 同一联系人重复次数校验
     */
    public AdmissionResultDTO verifyRepeatContactPhone(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || !rule.getSetting().containsKey("repeatPerson")
                || !rule.getSetting().containsKey("repeatNum")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {

            Integer repeatPerson = Integer.valueOf(rule.getSetting().get("repeatPerson"));//出现重复的人数
            Integer repeatNum = Integer.valueOf(rule.getSetting().get("repeatNum"));// 单个号码重复次数

            Map param = new HashMap();
            param.put("repeatNum", repeatNum);
            param.put("nid", request.getNid());
            int repeatPersonCur = clientContactDao.getRepeatPersons(param);
            result.setData(repeatPersonCur);
            // 校验
            if (repeatPerson.intValue() <= repeatPersonCur) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;

        } catch (Exception e) {
            log.error("[决策校验-同一联系人重复次数校验异常]：单号：{}", request.getNid(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("同一联系人重复次数校验失败");
            return result;
        }
    }

    /**
     * 新户-验证30天内运营商所有通话次数
     * 新户-验证30天内通讯录在运营商有效通话次数
     * 老户-验证30天内运营商互相通话次数
     * {"oldDays": "7","oldPassNum":"1","newDays": "30","newCntPassNum": "10","newAllPassNum":"100"}
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verify30DaysCallDetail(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || !rule.getSetting().containsKey("newDays")
                || !rule.getSetting().containsKey("newCntPassNum")
                || !rule.getSetting().containsKey("newAllPassNum")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {
            int oldDays = Integer.valueOf(rule.getSetting().get("oldDays"));//
            int newDays = Integer.valueOf(rule.getSetting().get("newDays"));//
            int oldPassNum = Integer.valueOf(rule.getSetting().get("oldPassNum"));//
            int newCntPassNum = Integer.valueOf(rule.getSetting().get("newCntPassNum"));//
            int newAllPassNum = Integer.valueOf(rule.getSetting().get("newAllPassNum"));//

            // 新户
            if (DecisionHandleRequest.LABLEGROUPIDNEW_1.equals(request.getLabelGroupId())) {

                Map<String, Object> param = new HashMap<>();
                param.put("userId", request.getUserId());
                param.put("nid", request.getNid());
                param.put("applyTime", request.getApplyTime());
                param.put("days", newDays);

                int cntCallNum30 = clientContactDao.getValidCallDetail(param);
                result.setData(cntCallNum30);

                if (newCntPassNum > cntCallNum30) {
                    result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                    return result;
                }

                int allCallNum30 = clientContactDao.getAllCallDetail(param);
                result.setData(allCallNum30);
                if (newAllPassNum > allCallNum30) {
                    result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                    return result;
                }
            }
            // 老户
            else if (DecisionHandleRequest.lableGroupIdOld.equals(request.getLabelGroupId())) {

                Map<String, Object> param = new HashMap<>();
                param.put("userId", request.getUserId());
                param.put("nid", request.getNid());
                param.put("applyTime", request.getApplyTime());
                param.put("days", oldDays);

                int count = clientContactDao.getAllCallDetail(param);
                result.setData(count);
                if (oldPassNum > count) {
                    result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                    return result;
                }
            }
            // 其他
            else {
                result.setData(request.getLabelGroupId());
                result.setResult(AdmissionResultDTO.RESULT_SKIP);
                return result;
            }
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;

        } catch (Exception e) {
            log.error("[决策1060异常]：单号：{}", request.getNid(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("决策1060异常");
            return result;
        }
    }


    /**
     * 1061验证最大逾期天数
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyMaxOverdueDay(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        if (null == rule
                || !rule.getSetting().containsKey("maxOverdueDay")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("规则为空，跳过");
            return result;
        }

        try {
            Integer ruleMaxOverdueDay = Integer.valueOf(rule.getSetting().get("maxOverdueDay"));//历史最大逾期天数
            Integer maxOverdueDay = request.getMaxOverdueDay();
            maxOverdueDay = null == maxOverdueDay ? 0 : maxOverdueDay;

            result.setData(maxOverdueDay);
            if (maxOverdueDay >= ruleMaxOverdueDay) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            } else {
                result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                return result;
            }
        } catch (Exception e) {
            log.error("[决策1060异常]：单号：{}", request.getNid(), e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("决策1060异常");
            return result;
        }
    }
}

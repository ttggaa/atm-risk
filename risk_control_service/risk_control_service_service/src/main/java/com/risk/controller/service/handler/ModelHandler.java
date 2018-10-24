package com.risk.controller.service.handler;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.DateTools;
import com.risk.controller.service.common.utils.PhoneUtils;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.util.AdmissionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 排序
 */
@Slf4j
@Service
public class ModelHandler implements AdmissionHandler {
    @Autowired
    private ModelRuleDetailDao modelRuleDetailDao;
    @Autowired
    private ModelRuleDetailScopeDao modelRuleDetailScopeDao;
    @Autowired
    private UserContactNumDao userContactNumDao;
    @Autowired
    private ModelGroupDao modelGroupDao;
    @Autowired
    private ModelGroupRuleDao modelGroupRuleDao;
    @Autowired
    private MongoHandler mongoHandler;
    @Autowired
    private ModelOperatorReportDao modelOperatorReportDao;
    @Autowired
    private ModelUserScoreDao modelUserScoreDao;

    /**
     * 计算用户的运营商数据
     *
     * @param request
     * @param rule
     * @return
     */
    public AdmissionResultDTO verifyUserOperator(DecisionHandleRequest request, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();
        try {
            // 紧急联系人
            List<JSONObject> deviceMainContact = this.mongoHandler.getUserMainContact(request);
            if (null == deviceMainContact || deviceMainContact.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到联系人信息");
                return result;
            }
            Set<String> setMainContact = new HashSet<>();
            deviceMainContact.forEach(json -> {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    setMainContact.add(phone);
                }
            });

            // 查询设备通讯录信息
            List<JSONObject> deviceContact = this.mongoHandler.getUserDeviceContact(request);
            if (null == deviceMainContact || deviceMainContact.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到设备通讯录信息");
                return result;
            }
            Set<String> setContact = new HashSet<>();
            for (JSONObject json : deviceContact) {
                String phone = json.getString("contactsPhone");
                phone = PhoneUtils.cleanTel(phone);
                if (PhoneUtils.isMobile(phone)) {
                    setContact.add(phone);
                }
            }

            // 查询运营商通话记录
            List<JSONObject> operatorCallDetail = this.mongoHandler.getUserOperatorCallDetail(request);
            if (null == operatorCallDetail || operatorCallDetail.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_SUSPEND);
                result.setData("未查询到运营商通话记录");
                return result;
            }

            List<ModelOperatorReport> listResult = this.saveOperatorReport(request, setContact, setMainContact, operatorCallDetail);

            if (null == listResult || listResult.size() <= 0) {
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                result.setData("没有通话、短信数据");
                return result;
            }
            return this.verifyModelV2(request, listResult, rule);

        } catch (Exception e) {
            log.error("计算用户的运营商数据，request;{},error", request, e);
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            result.setData("程序异常");
        }
        result.setResult(AdmissionResultDTO.RESULT_APPROVED);
        return result;
    }

    /**
     * 第二版模型
     *
     * @param request
     * @param listResult
     * @param rule
     * @return
     */
    private AdmissionResultDTO verifyModelV2(DecisionHandleRequest request, List<ModelOperatorReport> listResult, AdmissionRuleDTO rule) {
        AdmissionResultDTO result = new AdmissionResultDTO();

        if (null == rule || rule.getSetting() == null
                || !rule.getSetting().containsKey("minPercent")
                || !rule.getSetting().containsKey("maxPercent")
                || !rule.getSetting().containsKey("minTimePercent")
                || !rule.getSetting().containsKey("maxTimePercent")
                || !rule.getSetting().containsKey("allCallNum30")
                || !rule.getSetting().containsKey("cntCallNum30")
                || !rule.getSetting().containsKey("num30Compare180")
                || !rule.getSetting().containsKey("time30Compare180")) {

            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            return result;
        }

        try {
            Double cntCallTime = 0D; // 联系人通话总时长
            Double allCallTime = 0D; // 所有通话总时长
            Double cntCallNum = 0D; // 联系人通话次数
            Double allCallNum = 0D; // 所有通话总次数
            Double allCallNum30 = 0D; // 最近30天所有通话次数
            Double cntCallNum30 = 0D; // 最近30天联系人通话次数
            Double allCallTime30 = 0D; // 最近30天所有通话时长
            Double cntCallTime30 = 0D; // 最近30天联系人通话时长

            String data30 = DateTools.addDay(new Date(request.getApplyTime()), -30);
            for (ModelOperatorReport operatorReport : listResult) {
                cntCallTime += operatorReport.getCntCallTimeIn() + operatorReport.getCntCallTimeOut() + operatorReport.getCntCallTimeUn();
                allCallTime += operatorReport.getAllCallTimeIn() + operatorReport.getAllCallTimeOut() + operatorReport.getAllCallTimeUn();
                cntCallNum += operatorReport.getCntCallNumIn() + operatorReport.getCntCallNumOut() + operatorReport.getCntCallNumUn();
                allCallNum += operatorReport.getAllCallNumIn() + operatorReport.getAllCallNumOut() + operatorReport.getAllCallNumUn();

                if (DateTools.getDayDiff(DateTools.convert(data30), DateTools.convert(operatorReport.getDate())) >= 0) {
                    allCallNum30 += operatorReport.getAllCallNumIn() + operatorReport.getAllCallNumOut() + operatorReport.getAllCallNumUn();
                    cntCallNum30 += operatorReport.getCntCallNumIn() + operatorReport.getCntCallNumOut() + operatorReport.getCntCallNumUn();
                    allCallTime30 += operatorReport.getAllCallTimeIn() + operatorReport.getAllCallTimeOut() + operatorReport.getAllCallTimeUn();
                    cntCallTime30 += operatorReport.getCntCallTimeIn() + operatorReport.getCntCallTimeOut() + operatorReport.getCntCallTimeUn();
                }
            }

            Double ruleAllCallNum30 = Double.valueOf(rule.getSetting().get("allCallNum30"));
            Double ruleCntCallNum30 = Double.valueOf(rule.getSetting().get("cntCallNum30"));
            if (allCallNum30 < ruleAllCallNum30) {
                result.setData("30天总通话次数:" + allCallNum30);
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }
            if (cntCallNum30 < ruleCntCallNum30) {
                result.setData("30天联系人通话次数:" + cntCallNum30);
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }

            if (allCallTime == 0 || allCallNum == 0 || cntCallTime == 0 || cntCallNum == 0) {
                result.setData("通话时长通话次数为0");
                result.setResult(AdmissionResultDTO.RESULT_REJECTED);
                return result;
            }

            Double callTimePercent = cntCallTime / allCallTime;//通话时长比率
            Double callNumPercent = cntCallNum / allCallNum;//通话次数比率
            Double timeAndNumPercent = callTimePercent / callNumPercent;// 通话时长比率/通话次数比率

            Double ruleMinPercent = Double.valueOf(rule.getSetting().get("minPercent"));
            Double ruleMaxPercent = Double.valueOf(rule.getSetting().get("maxPercent"));
            Double ruleMinTimePercent = Double.valueOf(rule.getSetting().get("minTimePercent"));
            Double ruleMaxTimePercent = Double.valueOf(rule.getSetting().get("maxTimePercent"));
            Double ruleNum30Compare180 = Double.valueOf(rule.getSetting().get("num30Compare180"));//30天通话次数比值与180天通话次数比值差值
            Double ruleTime30Compare180 = Double.valueOf(rule.getSetting().get("time30Compare180"));//30天通话时长比值与180天通话时长比值差值

            // 1、计算比值<=1.15 拒绝，比值在1.15至1.6（时长比值>=0.65过，0.5至0.65挂起，<=0.6拒绝）电核， 比值 >=1.6电核
            // 大约最大比值，人工审核
            result.setData(new BigDecimal(timeAndNumPercent).setScale(5, BigDecimal.ROUND_HALF_UP));
            if (timeAndNumPercent.compareTo(ruleMaxPercent) >= 0) {
                result.setResult(AdmissionResultDTO.RESULT_MANUAL);
            }
            // >=最小值，小于最大值，验证通话次数
            else if (timeAndNumPercent.compareTo(ruleMinPercent) >= 0 && timeAndNumPercent.compareTo(ruleMaxPercent) < 0) {

                result.setData(new BigDecimal(callTimePercent).setScale(5, BigDecimal.ROUND_HALF_UP));
                if (callTimePercent.compareTo(ruleMaxTimePercent) >= 0) {
                    result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                }
                // 通话次数>=最小值，<最大值
                else if (callTimePercent.compareTo(ruleMinTimePercent) >= 0 && callTimePercent.compareTo(ruleMaxTimePercent) < 0) {
                    result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                }
                // callTimePercent < 最小值
                else {
                    result.setResult(AdmissionResultDTO.RESULT_MANUAL);
                }
            }
            // timeAndNumPercent < 最小值
            else {
                result.setResult(AdmissionResultDTO.RESULT_MANUAL);
            }

            // 2、30天比值 与180天比值 在+-0.1之间过
            if (result.getResult() != AdmissionResultDTO.RESULT_MANUAL
                    && allCallTime30 > 0 && allCallNum30 > 0 && cntCallTime30 > 0 && cntCallNum30 > 0) {

                Double callTimePercent30 = cntCallTime30 / allCallTime30; //30天通话时长比率
                Double callNumPercent30 = cntCallNum30 / allCallNum30; //30天通话次数比率

                if (Math.abs(callTimePercent30 - callTimePercent) <= ruleTime30Compare180
                        && Math.abs(callNumPercent30 - callNumPercent) <= ruleNum30Compare180) {

                    result.setData("±");
                    result.setResult(AdmissionResultDTO.RESULT_APPROVED);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("计算用户运营商数据异常,request:{}", request, e);
            result.setData("程序异常");
            result.setResult(AdmissionResultDTO.RESULT_EXCEPTIONAL);
            return result;
        }
    }

    /**
     * 计算用户运营商通话次数、通话时长，短信发送次数等，并写表
     *
     * @param request
     * @param setContact         设备通讯录手机号码集合
     * @param setMainContact     紧急联系人集合
     * @param operatorCallDetail 运营商通话记录
     * @return
     */
    private List<ModelOperatorReport> saveOperatorReport(DecisionHandleRequest request, Set<String> setContact, Set<String> setMainContact, List<JSONObject> operatorCallDetail) {
        // 保存数据
        List<ModelOperatorReport> listResult = new ArrayList<>();
        try {
            Map<Date, ModelOperatorReport> map = new TreeMap<>();//安日期升序排列
            for (JSONObject jsonObject : operatorCallDetail) {

                String strTime = jsonObject.getString("time");// 通话时间
                String peerNumber = jsonObject.getString("peer_number");//通话手机号码
                Long duration = jsonObject.getLong("duration");//通话时长
                String dialType = jsonObject.getString("dial_type");//通话类型，主叫被叫

                if (StringUtils.isBlank(peerNumber) || StringUtils.isBlank(strTime) || null == duration || duration == 0) {
                    continue;
                }

                Date time = DateTools.convert(jsonObject.getString("time"));

                ModelOperatorReport report = null;
                if (map.containsKey(time)) {
                    report = map.get(time);
                } else {
                    report = new ModelOperatorReport();
                    report.setUserId(request.getUserId());
                    report.setDate(DateTools.convert(time, "yyyy-MM-dd HH"));
                    report.setNid(request.getNid());
                }

                // 所有联系人通话次数和时长
                // 被叫
                if ("DIALED".equals(dialType)) {
                    report.setAllCallNumIn(report.getAllCallNumIn() + 1);
                    report.setAllCallTimeIn(report.getAllCallTimeIn() + duration);
                }
                // 被叫
                else if ("DIAL".equals(dialType)) {
                    report.setAllCallNumOut(report.getAllCallNumOut() + 1);
                    report.setAllCallTimeOut(report.getAllCallTimeOut() + duration);
                }
                // 未知
                else {
                    report.setAllCallNumUn(report.getAllCallNumUn() + 1);
                    report.setAllCallTimeUn(report.getAllCallTimeUn() + duration);
                }

                // 紧急联系人通话次数和时长
                if (setMainContact.contains(peerNumber)) {
                    if ("DIALED".equals(dialType)) {
                        report.setMainCallNumIn(report.getMainCallNumIn() + 1);
                        report.setMainCallTimeIn(report.getMainCallTimeIn() + duration);
                    } else if ("DIAL".equals(dialType)) {
                        report.setMainCallNumOut(report.getMainCallNumOut() + 1);
                        report.setMainCallTimeOut(report.getMainCallTimeOut() + duration);
                    } else {
                        report.setMainCallNumUn(report.getMainCallNumUn() + 1);
                        report.setMainCallTimeUn(report.getMainCallTimeUn() + duration);
                    }
                }

                // 通讯录
                if (setContact.contains(peerNumber)) {
                    if ("DIALED".equals(dialType)) {
                        report.setCntCallNumIn(report.getCntCallNumIn() + 1);
                        report.setCntCallTimeIn(report.getCntCallTimeIn() + duration);
                    } else if ("DIAL".equals(dialType)) {
                        report.setCntCallNumOut(report.getCntCallNumOut() + 1);
                        report.setCntCallTimeOut(report.getCntCallTimeOut() + duration);
                    } else {
                        report.setCntCallNumUn(report.getCntCallNumUn() + 1);
                        report.setCntCallTimeUn(report.getCntCallTimeUn() + duration);
                    }
                }
                map.put(time, report);
            }

            // 查询运营商通话记录
            List<JSONObject> operatorSms = this.mongoHandler.getUserOperatorSms(request);
            if (null != operatorSms && operatorSms.size() > 0) {

                for (JSONObject jsonObject : operatorSms) {

                    String strTime = jsonObject.getString("time");// 短信时间
                    String peerNumber = jsonObject.getString("peer_number");//短信手机号码
                    String sendType = jsonObject.getString("send_type");//短信类型：RECEIVE，SEND

                    if (StringUtils.isBlank(peerNumber) || !PhoneUtils.isMobile(peerNumber) || StringUtils.isBlank(strTime)) {
                        continue;
                    }

                    Date time = null;
                    try {
                        time = DateTools.convert(jsonObject.getString("time"), "yyyy-MM-dd HH");
                    } catch (Exception e) {
                        log.error("日期转换失败,time:{}", jsonObject.getString("time"));
                        continue;
                    }

                    ModelOperatorReport report = null;
                    if (map.containsKey(time)) {
                        report = map.get(time);
                    } else {
                        report = new ModelOperatorReport();
                        report.setUserId(request.getUserId());
                        report.setDate(DateTools.convert(time, "yyyy-MM-dd HH"));
                        report.setNid(request.getNid());
                    }

                    // 所有短信发送次数
                    if (("RECEIVE").equals(sendType)) {
                        report.setAllSmsNumIn(report.getAllSmsNumIn() + 1);
                    } else if (("SEND").equals(sendType)) {
                        report.setAllSmsNumOut(report.getAllSmsNumOut() + 1);
                    } else {
                        report.setAllSmsNumUn(report.getAllSmsNumUn() + 1);
                    }

                    // 紧急联系人通话次数和时长
                    if (setContact.contains(peerNumber)) {
                        if (("RECEIVE").equals(sendType)) {
                            report.setContactSmsNumIn(report.getContactSmsNumIn() + 1);
                        } else if (("SEND").equals(sendType)) {
                            report.setContactSmsNumOut(report.getContactSmsNumOut() + 1);
                        } else {
                            report.setContactSmsNumUn(report.getContactSmsNumUn() + 1);
                        }
                    }
                    map.put(time, report);
                }
            }
            map.forEach((k, v) -> listResult.add(v));
            if (listResult.size() > 0) {
                modelOperatorReportDao.saveBatch(listResult);
            }
        } catch (Exception e) {
            log.error("计算用户运营商数据异常,request:{}", request, e);
        }
        return listResult;
    }


    public AdmissionResultDTO verifyModel(DecisionHandleRequest request, List<ModelOperatorReport> listResult) {
        AdmissionResultDTO result = new AdmissionResultDTO();

        // 查询用户组对应的模型总分和通过分数及规则
        ModelGroup group = modelGroupDao.getByGroupId(request.getLabelGroupId());
        if (null == group) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("未查询到模型组信息");
            return result;
        }

        // 总分数
        BigDecimal totalSocre = group.getTotalSocre();
        BigDecimal ONE = new BigDecimal(1);

        // 用户组对应的模型规则
        List<ModelGroupRule> listModelGroupRule = modelGroupRuleDao.getByGroupId(request.getLabelGroupId());
        if (listModelGroupRule.size() <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_SKIP);
            result.setData("未查询到模型规则");
            return result;
        }
        // 计算结果
        UserContactNum dto = this.getUserContacttNum(listResult, request);

        BigDecimal userScore = totalSocre;//用户得分
        List<ModelUserScore> listSub = new ArrayList<>(); //保存用户扣分结果

        for (ModelGroupRule groupRule : listModelGroupRule) {
            // 查询模型规则下的规则明细
            List<ModelRuleDetail> ruleDetails = modelRuleDetailDao.getAll(groupRule.getRuleId());
            if (null == ruleDetails || ruleDetails.size() <= 0) {
                continue;
            }
            BigDecimal subScore = BigDecimal.ZERO;//减分制

            for (ModelRuleDetail modelRuleDetail : ruleDetails) {
                // 规则明细的范围
                List<ModelRuleDetailScope> listScopeRule = modelRuleDetailScopeDao.getScopeByRuleId(modelRuleDetail.getDetailId());
                if (null == listScopeRule || listScopeRule.size() <= 0) {
                    continue;
                }

                /** 紧急联系人通话次数、时长 start **/
                if (ModelRuleDetail.DETAIL_1001.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getNumCallIn30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getNumCallIn60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getNumCallIn90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getNumCallIn120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getNumCallIn150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getNumCallIn180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_1002.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getNumCallOut30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getNumCallOut60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getNumCallOut90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getNumCallOut120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getNumCallOut150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getNumCallOut180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_1003.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getNumCallUnKonw30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getNumCallUnKonw60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getNumCallUnKonw90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getNumCallUnKonw120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getNumCallUnKonw150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getNumCallUnKonw180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_1004.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallIn30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallIn60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallIn90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallIn120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallIn150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallIn180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_1005.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallOut30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallOut60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallOut90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallOut120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallOut150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallOut180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_1006.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallUnKonw30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallUnKonw60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallUnKonw90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallUnKonw120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallUnKonw150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getTimeCallUnKonw180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                /** 紧急联系人通话次数、时长 end **/

                /** 连续未通话/断续未通话 start **/
                else if (ModelRuleDetail.DETAIL_2001.equals(modelRuleDetail.getDetailId())) {
                    BigDecimal finalWeight = BigDecimal.ZERO;//权重
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        // 最近30天，连续48小时未通话，扣分
                        if (ModelRuleDetailScope.SCOPE_48.equals(scopeRule.getKey()) && dto.getContinueDiffDays30() * 24 >= scopeRule.getNum()) {
                            finalWeight = scopeRule.getWeight();
                        }
                        // 最近30天，断续未通话扣分
                        else if (dto.getInterruptDiffDays30() >= scopeRule.getNum()) {
                            finalWeight = finalWeight.add(scopeRule.getWeight());
                        }
                    }
                    finalWeight = finalWeight.compareTo(ONE) >= 0 ? ONE : finalWeight;
                    if (finalWeight.compareTo(BigDecimal.ZERO) > 0) {
                        subScore = subScore.add(finalWeight.multiply(groupRule.getWeight()).multiply(modelRuleDetail.getWeight()).multiply(totalSocre).setScale(8, BigDecimal.ROUND_HALF_UP));
                    }
                }

                /** 连续未通话/断续未通话 end **/
                else if (ModelRuleDetail.DETAIL_3001.equals(modelRuleDetail.getDetailId())) {
                    BigDecimal finalWeight = BigDecimal.ZERO;//权重
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (scopeRule.getNum() > (dto.getAllSmsNumIn() + dto.getAllSmsNumOut() + dto.getAllSmsNumUnKonow())) {
                            finalWeight = finalWeight.add(scopeRule.getWeight());
                        }
                    }
                    finalWeight = finalWeight.compareTo(ONE) >= 0 ? ONE : finalWeight;
                    if (finalWeight.compareTo(BigDecimal.ZERO) > 0) {
                        subScore = subScore.add(finalWeight.multiply(groupRule.getWeight()).multiply(modelRuleDetail.getWeight()).multiply(totalSocre).setScale(8, BigDecimal.ROUND_HALF_UP));
                    }
                }

                /** 连续未通话/断续未通话 end **/
                else if (ModelRuleDetail.DETAIL_3002.equals(modelRuleDetail.getDetailId())) {
                    BigDecimal finalWeight = BigDecimal.ZERO;//权重
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (scopeRule.getNum() > (dto.getContactSmsNumIn() + dto.getContactSmsNumOut() + dto.getContactSmsNumUnKonow())) {
                            finalWeight = finalWeight.add(scopeRule.getWeight());
                        }
                    }
                    finalWeight = finalWeight.compareTo(ONE) >= 0 ? ONE : finalWeight;
                    if (finalWeight.compareTo(BigDecimal.ZERO) > 0) {
                        subScore = subScore.add(finalWeight.multiply(groupRule.getWeight()).multiply(modelRuleDetail.getWeight()).multiply(totalSocre).setScale(8, BigDecimal.ROUND_HALF_UP));
                    }
                }

                /** 通讯录通话次数、时长 start **/
                if (ModelRuleDetail.DETAIL_4001.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumIn30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumIn60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumIn90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumIn120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumIn150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumIn180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_4002.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumOut30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumOut60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumOut90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumOut120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumOut150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumOut180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_4003.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumUnKnow30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumUnKnow60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumUnKnow90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumUnKnow120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumUnKnow150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getCntCallNumUnKnow180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_4004.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeIn30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeIn60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeIn90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeIn120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeIn150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeIn180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_4005.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeOut30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeOut60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeOut90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeOut120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeOut150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeOut180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
                //
                else if (ModelRuleDetail.DETAIL_4006.equals(modelRuleDetail.getDetailId())) {
                    for (ModelRuleDetailScope scopeRule : listScopeRule) {
                        if (ModelRuleDetailScope.SCOPEDAYS_30.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeUnKnow30().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_60.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeUnKnow60().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_90.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeUnKnow90().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_120.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeUnKnow120().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_150.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeUnKnow150().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        } else if (ModelRuleDetailScope.SCOPEDAYS_180.equals(scopeRule.getKey())) {
                            if (dto.getCntCallTimeUnKnow180().compareTo(scopeRule.getNum()) < 0) {
                                subScore = subScore.add(this.sumScore(totalSocre, groupRule, modelRuleDetail, scopeRule));
                            }
                        }
                    }
                }
            }


            userScore = userScore.subtract(subScore);

            ModelUserScore modelUserScore = new ModelUserScore();
            modelUserScore.setUserId(request.getUserId());
            modelUserScore.setNid(request.getNid());
            modelUserScore.setRuleId(groupRule.getRuleId());
            modelUserScore.setScore(userScore);
            modelUserScore.setSubScore(subScore);
            listSub.add(modelUserScore);
        }

        ModelUserScore modelUserScore = new ModelUserScore();
        modelUserScore.setUserId(request.getUserId());
        modelUserScore.setNid(request.getNid());
        modelUserScore.setRuleId(1000L);
        modelUserScore.setScore(userScore);
        modelUserScore.setSubScore(totalSocre.subtract(userScore));
        listSub.add(modelUserScore);
        try {
            if (null != listSub && listSub.size() > 0) {
                modelUserScoreDao.saveBatch(listSub);
            }
        } catch (Exception e) {
            log.error("插入用户分数失败，listSub：{}，e:", JSONObject.toJSONString(listSub), e);
        }
        result.setData(userScore);
        if (group.getPassScore().compareTo(userScore) <= 0) {
            result.setResult(AdmissionResultDTO.RESULT_APPROVED);
            return result;
        } else {
            result.setResult(AdmissionResultDTO.RESULT_REJECTED);
            return result;
        }

    }

    /**
     * 计算减分项
     *
     * @param totalSocre      总分
     * @param groupRule       规则
     * @param modelRuleDetail 规则明细
     * @param scopeRule       规则明细区间规则
     * @return
     */
    private BigDecimal sumScore(BigDecimal totalSocre, ModelGroupRule groupRule, ModelRuleDetail modelRuleDetail, ModelRuleDetailScope scopeRule) {
        BigDecimal diffScore = totalSocre.multiply(groupRule.getWeight()).multiply(modelRuleDetail.getWeight()).multiply(scopeRule.getWeight()).setScale(8, BigDecimal.ROUND_HALF_UP);
        return diffScore;
    }

    /**
     * 计算30天内用户通话时长
     *
     * @param listResult
     * @param request
     * @return
     */
    private UserContactNum getUserContacttNum(List<ModelOperatorReport> listResult, DecisionHandleRequest request) {

        Date applyDate = new Date(request.getApplyTime());
        UserContactNum userContactNum = new UserContactNum();

        Set<Date> setDate = new TreeSet<>();//日期按升序排列
        for (int i = 0; i < listResult.size(); i++) {
            ModelOperatorReport operatorReport = listResult.get(i);
            // 申请时间与通话时间或者发送短信时间差的绝对值
            Long applyDayDiff = Math.abs(DateTools.getDayDiff(DateTools.convert(operatorReport.getDate(), "yyyy-MM-dd"), applyDate));

            // 1、计算和紧急联系人最近30天、60天、90天、120天、150天、180天通话时长和通话次数
            // 60天等通话时长累加
            if (applyDayDiff < 30) {
                userContactNum.setNumCallIn30(userContactNum.getNumCallIn30() + operatorReport.getMainCallNumIn());
                userContactNum.setNumCallOut30(userContactNum.getNumCallOut30() + operatorReport.getMainCallNumOut());
                userContactNum.setNumCallUnKonw30(userContactNum.getNumCallUnKonw30() + operatorReport.getMainCallNumUn());
                userContactNum.setTimeCallIn30(userContactNum.getTimeCallIn30() + operatorReport.getMainCallTimeIn());
                userContactNum.setTimeCallOut30(userContactNum.getTimeCallOut30() + operatorReport.getMainCallTimeOut());
                userContactNum.setTimeCallUnKonw30(userContactNum.getTimeCallUnKonw30() + operatorReport.getMainCallTimeUn());

                userContactNum.setCntCallNumIn30(userContactNum.getCntCallNumIn30() + operatorReport.getCntCallNumIn());
                userContactNum.setCntCallNumOut30(userContactNum.getCntCallNumOut30() + operatorReport.getCntCallNumOut());
                userContactNum.setCntCallNumUnKnow30(userContactNum.getCntCallNumUnKnow30() + operatorReport.getCntCallNumUn());
                userContactNum.setCntCallTimeIn30(userContactNum.getCntCallTimeIn30() + operatorReport.getCntCallTimeIn());
                userContactNum.setCntCallTimeOut30(userContactNum.getCntCallTimeOut30() + operatorReport.getCntCallTimeOut());
                userContactNum.setCntCallTimeUnKnow30(userContactNum.getCntCallTimeUnKnow30() + operatorReport.getCntCallTimeUn());

                // 计算最近30天内，多少天连续未通话的
                setDate.add(DateTools.convert(listResult.get(i).getDate(), "yyyy-MM-dd"));
            }
            if (applyDayDiff < 60) {
                userContactNum.setNumCallIn60(userContactNum.getNumCallIn60() + operatorReport.getMainCallNumIn());
                userContactNum.setNumCallOut60(userContactNum.getNumCallOut60() + operatorReport.getMainCallNumOut());
                userContactNum.setNumCallUnKonw60(userContactNum.getNumCallUnKonw60() + operatorReport.getMainCallNumUn());
                userContactNum.setTimeCallIn60(userContactNum.getTimeCallIn60() + operatorReport.getMainCallTimeIn());
                userContactNum.setTimeCallOut60(userContactNum.getTimeCallOut60() + operatorReport.getMainCallTimeOut());
                userContactNum.setTimeCallUnKonw60(userContactNum.getTimeCallUnKonw60() + operatorReport.getMainCallTimeUn());

                userContactNum.setCntCallNumIn60(userContactNum.getCntCallNumIn60() + operatorReport.getCntCallNumIn());
                userContactNum.setCntCallNumOut60(userContactNum.getCntCallNumOut60() + operatorReport.getCntCallNumOut());
                userContactNum.setCntCallNumUnKnow60(userContactNum.getCntCallNumUnKnow60() + operatorReport.getCntCallNumUn());
                userContactNum.setCntCallTimeIn60(userContactNum.getCntCallTimeIn60() + operatorReport.getCntCallTimeIn());
                userContactNum.setCntCallTimeOut60(userContactNum.getCntCallTimeOut60() + operatorReport.getCntCallTimeOut());
                userContactNum.setCntCallTimeUnKnow60(userContactNum.getCntCallTimeUnKnow60() + operatorReport.getCntCallTimeUn());
            }
            if (applyDayDiff < 90) {
                userContactNum.setNumCallIn90(userContactNum.getNumCallIn90() + operatorReport.getMainCallNumIn());
                userContactNum.setNumCallOut90(userContactNum.getNumCallOut90() + operatorReport.getMainCallNumOut());
                userContactNum.setNumCallUnKonw90(userContactNum.getNumCallUnKonw90() + operatorReport.getMainCallNumUn());
                userContactNum.setTimeCallIn90(userContactNum.getTimeCallIn90() + operatorReport.getMainCallTimeIn());
                userContactNum.setTimeCallOut90(userContactNum.getTimeCallOut90() + operatorReport.getMainCallTimeOut());
                userContactNum.setTimeCallUnKonw90(userContactNum.getTimeCallUnKonw90() + operatorReport.getMainCallTimeUn());

                userContactNum.setCntCallNumIn90(userContactNum.getCntCallNumIn90() + operatorReport.getCntCallNumIn());
                userContactNum.setCntCallNumOut90(userContactNum.getCntCallNumOut90() + operatorReport.getCntCallNumOut());
                userContactNum.setCntCallNumUnKnow90(userContactNum.getCntCallNumUnKnow90() + operatorReport.getCntCallNumUn());
                userContactNum.setCntCallTimeIn90(userContactNum.getCntCallTimeIn90() + operatorReport.getCntCallTimeIn());
                userContactNum.setCntCallTimeOut90(userContactNum.getCntCallTimeOut90() + operatorReport.getCntCallTimeOut());
                userContactNum.setCntCallTimeUnKnow90(userContactNum.getCntCallTimeUnKnow90() + operatorReport.getCntCallTimeUn());
            }
            if (applyDayDiff < 120) {
                userContactNum.setNumCallIn120(userContactNum.getNumCallIn120() + operatorReport.getMainCallNumIn());
                userContactNum.setNumCallOut120(userContactNum.getNumCallOut120() + operatorReport.getMainCallNumOut());
                userContactNum.setNumCallUnKonw120(userContactNum.getNumCallUnKonw120() + operatorReport.getMainCallNumUn());
                userContactNum.setTimeCallIn120(userContactNum.getTimeCallIn120() + operatorReport.getMainCallTimeIn());
                userContactNum.setTimeCallOut120(userContactNum.getTimeCallOut120() + operatorReport.getMainCallTimeOut());
                userContactNum.setTimeCallUnKonw120(userContactNum.getTimeCallUnKonw120() + operatorReport.getMainCallTimeUn());

                userContactNum.setCntCallNumIn120(userContactNum.getCntCallNumIn120() + operatorReport.getCntCallNumIn());
                userContactNum.setCntCallNumOut120(userContactNum.getCntCallNumOut120() + operatorReport.getCntCallNumOut());
                userContactNum.setCntCallNumUnKnow120(userContactNum.getCntCallNumUnKnow120() + operatorReport.getCntCallNumUn());
                userContactNum.setCntCallTimeIn120(userContactNum.getCntCallTimeIn120() + operatorReport.getCntCallTimeIn());
                userContactNum.setCntCallTimeOut120(userContactNum.getCntCallTimeOut120() + operatorReport.getCntCallTimeOut());
                userContactNum.setCntCallTimeUnKnow120(userContactNum.getCntCallTimeUnKnow120() + operatorReport.getCntCallTimeUn());
            }
            if (applyDayDiff < 150) {
                userContactNum.setNumCallIn150(userContactNum.getNumCallIn150() + operatorReport.getMainCallNumIn());
                userContactNum.setNumCallOut150(userContactNum.getNumCallOut150() + operatorReport.getMainCallNumOut());
                userContactNum.setNumCallUnKonw150(userContactNum.getNumCallUnKonw150() + operatorReport.getMainCallNumUn());
                userContactNum.setTimeCallIn150(userContactNum.getTimeCallIn150() + operatorReport.getMainCallTimeIn());
                userContactNum.setTimeCallOut150(userContactNum.getTimeCallOut150() + operatorReport.getMainCallTimeOut());
                userContactNum.setTimeCallUnKonw150(userContactNum.getTimeCallUnKonw150() + operatorReport.getMainCallTimeUn());

                userContactNum.setCntCallNumIn150(userContactNum.getCntCallNumIn150() + operatorReport.getCntCallNumIn());
                userContactNum.setCntCallNumOut150(userContactNum.getCntCallNumOut150() + operatorReport.getCntCallNumOut());
                userContactNum.setCntCallNumUnKnow150(userContactNum.getCntCallNumUnKnow150() + operatorReport.getCntCallNumUn());
                userContactNum.setCntCallTimeIn150(userContactNum.getCntCallTimeIn150() + operatorReport.getCntCallTimeIn());
                userContactNum.setCntCallTimeOut150(userContactNum.getCntCallTimeOut150() + operatorReport.getCntCallTimeOut());
                userContactNum.setCntCallTimeUnKnow150(userContactNum.getCntCallTimeUnKnow150() + operatorReport.getCntCallTimeUn());
            }
            if (applyDayDiff < 180) {
                userContactNum.setNumCallIn180(userContactNum.getNumCallIn180() + operatorReport.getMainCallNumIn());
                userContactNum.setNumCallOut180(userContactNum.getNumCallOut180() + operatorReport.getMainCallNumOut());
                userContactNum.setNumCallUnKonw180(userContactNum.getNumCallUnKonw180() + operatorReport.getMainCallNumUn());
                userContactNum.setTimeCallIn180(userContactNum.getTimeCallIn180() + operatorReport.getMainCallTimeIn());
                userContactNum.setTimeCallOut180(userContactNum.getTimeCallOut180() + operatorReport.getMainCallTimeOut());
                userContactNum.setTimeCallUnKonw180(userContactNum.getTimeCallUnKonw180() + operatorReport.getMainCallTimeUn());

                userContactNum.setCntCallNumIn180(userContactNum.getCntCallNumIn180() + operatorReport.getCntCallNumIn());
                userContactNum.setCntCallNumOut180(userContactNum.getCntCallNumOut180() + operatorReport.getCntCallNumOut());
                userContactNum.setCntCallNumUnKnow180(userContactNum.getCntCallNumUnKnow180() + operatorReport.getCntCallNumUn());
                userContactNum.setCntCallTimeIn180(userContactNum.getCntCallTimeIn180() + operatorReport.getCntCallTimeIn());
                userContactNum.setCntCallTimeOut180(userContactNum.getCntCallTimeOut180() + operatorReport.getCntCallTimeOut());
                userContactNum.setCntCallTimeUnKnow180(userContactNum.getCntCallTimeUnKnow180() + operatorReport.getCntCallTimeUn());
            }

            // 2、短信次数
            userContactNum.setAllSmsNumIn(userContactNum.getAllSmsNumIn() + operatorReport.getAllSmsNumIn());
            userContactNum.setAllSmsNumOut(userContactNum.getAllSmsNumOut() + operatorReport.getAllSmsNumOut());
            userContactNum.setAllSmsNumUnKonow(userContactNum.getAllSmsNumUnKonow() + operatorReport.getAllSmsNumUn());
            userContactNum.setContactSmsNumIn(userContactNum.getContactSmsNumIn() + operatorReport.getContactSmsNumIn());
            userContactNum.setContactSmsNumOut(userContactNum.getContactSmsNumOut() + operatorReport.getContactSmsNumOut());
            userContactNum.setContactSmsNumUnKonow(userContactNum.getContactSmsNumUnKonow() + operatorReport.getContactSmsNumUn());
        }

        // 计算30天内连续未通话的天数，和断续未通话天数
        userContactNum.setInterruptDiffDays30(Math.abs(30L - setDate.size()));
        List<Date> listDate = new ArrayList<>(setDate);
        for (int i = listDate.size() - 1; i >= 0; i--) {
            Long diffDays = 0L;

            // i==最大值时，日期是最大值
            if (i == listDate.size() - 1) {
                diffDays = Math.abs(DateTools.getDayDiff(listDate.get(i), applyDate));
            }
            // i==0时，日期是最小值,计算最小日期与30天之前的日期差多少天
            else if (i == 0) {
                diffDays = Math.abs(DateTools.getDayDiff(listDate.get(i), DateTools.addDayToDate(applyDate, -30)));
            } else {
                diffDays = Math.abs(DateTools.getDayDiff(listDate.get(i), listDate.get(i - 1)));
            }
            // 设置最大的连续未通话时间
            userContactNum.setContinueDiffDays30(diffDays > userContactNum.getContinueDiffDays30() ? diffDays : userContactNum.getContinueDiffDays30());
        }

        return userContactNum;
    }
}

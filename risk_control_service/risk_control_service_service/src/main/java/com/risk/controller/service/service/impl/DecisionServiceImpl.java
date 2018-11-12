package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.controller.service.common.constans.ERROR;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.*;
import com.risk.controller.service.dto.*;
import com.risk.controller.service.entity.*;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.mongo.dao.MongoDao;
import com.risk.controller.service.mongo.dao.MongoQuery;
import com.risk.controller.service.service.DecisionRobotService;
import com.risk.controller.service.service.DecisionService;
import com.risk.controller.service.utils.DataBaseUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * @Author ZT
 * @create 2018-08-27
 */
@Service
@Slf4j
public class DecisionServiceImpl implements DecisionService {

    @Autowired
    private DecisionRobotService robotService;

    @Autowired
    private DecisionRobotNoticeDao decisionRobotNoticeDao;

    @Autowired
    private AdmissionResultDao admissionResultDao;
    @Autowired
    private AdmissionResultDetailDao admissionResultDetailDao;

    @Autowired
    private LocalCache localCache;

    @Autowired
    private MongoDao mongoDao;

    @Autowired
    private DataBaseUtils dataBaseUtils;

    @Autowired
    private DecisionResultNoticeDao decisionResultNoticeDao;
    @Autowired
    private RejectReasonDao rejectReasonDao;

    private final ObjectMapper MAPPER = new ObjectMapper();

    private static Map<String, String> relationMap = new HashMap<>();

    static {
        relationMap.put("1", "父母");
        relationMap.put("2", "配偶");
        relationMap.put("3", "兄弟");
        relationMap.put("4", "姐妹");
        relationMap.put("5", "朋友");
    }

    @Override
    public ResponseEntity robotResultNotice(RobotResultRequest request, RobotScoreDTO robotScore, boolean isModelNotice) {
        // 查询异常次数
        if (robotScore == null) {
            robotScore = new RobotScoreDTO();
        }
        robotScore.setNid(request.getNid());
        robotScore.setStatus(request.getStatus());
        // 模型好人概率
        String probOf0 = request.getProb_of_0();
        if (StringUtils.isBlank(probOf0)) {
            probOf0 = "-1";
        }
        // 模型坏人概率
        String probOf2 = request.getProb_of_1();
        if (StringUtils.isBlank(probOf2)) {
            probOf2 = "-1";
        }
        robotScore.setProbOf0(new BigDecimal(Double.valueOf(probOf0)));
        robotScore.setProbOf1(new BigDecimal(Double.valueOf(probOf2)));
        robotScore.setErrorMsg(JSONObject.toJSONString(request));
        robotService.saveOut(robotScore);//保存输出

        // 状态异常结果
        Integer isEndRobotResult = robotNoticeError(request);
        if (Integer.compare(1, isEndRobotResult) == 0) {
            return new ResponseEntity(ResponseEntity.STATUS_OK);
        }
        // 处理下数据
        if (Integer.compare(2, isEndRobotResult) == 0) {
            // 设置失败
            request.setFinalResult("2");
            request.setPreliminaryResult("-1");
        }

        robotScore.setFinalResult(Integer.valueOf(request.getFinalResult()));
        robotScore.setPreliminaryResult(Integer.valueOf(request.getPreliminaryResult()));
        AdmissionResult findResult = new AdmissionResult();
        findResult.setNid(request.getNid());
        AdmissionResult result = admissionResultDao.getByNid(findResult);
        if (result == null) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL);
        }
        AdmissionResultDTO admResult = new AdmissionResultDTO();
        admResult.setResult(result.getResult());
        // 根据nid获取决策结果的拒绝原因编码
        List<String> rejectReasonList = admissionResultDao.getRejectReason(result.getId());
        Set<String> rejectReasonSet = new LinkedHashSet<String>();
        rejectReasonSet.addAll(rejectReasonList);
        admResult.setRejectReason(rejectReasonSet);
        admResult.setId(result.getId());
        admResult.setLabelGroupId(result.getLabelGroupId());
        // 合并机器人评分结果
        DecisionRobotResult decisionRobotResult = robotService.merge(admResult, robotScore, request);

        // 如果是模型处理异常
        if (Integer.compare(2, isEndRobotResult) == 0) {
            decisionRobotResult.setResult(2);
            // 添加模型异常
            rejectReasonSet.add("R1815"); //模型异常处理拒绝
        }

        // 保存 DecisionRobotResult
        robotService.saveDecisionRobotResult(decisionRobotResult);
        admResult.setResult(decisionRobotResult.getResult());

        if (isModelNotice) {
            // 通知对应的服务更改借款单审核分配表
            return noticeBorrowResultHandle(request.getNid(), admResult); // borrowResultHandle(request,admResult);
        } else {
            return new ResponseEntity(ResponseEntity.STATUS_OK);
        }
    }

    /**
     * 模型获取数据接口
     *
     * @param nid
     * @return
     */
    @Override
    public ResponseEntity getAllDataByNid(String nid) {
        // 根据nid查询用户和订单信息
        UserAndBorrowInfo userAndBorrowInfo = getUserAndBorrowInfoByNid(nid);
        if (userAndBorrowInfo == null) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, ERROR.ErrorCode.USER_BORROW_INFO, ERROR.ErrorMsg.USER_BORROW_INFO, null);
        }
        userAndBorrowInfo.setNid(nid);
        // 获取秒白条征信数据
        JSONObject jsonObject = getThirdPartyCredit(userAndBorrowInfo);
        if (jsonObject == null) {
            return new ResponseEntity(ResponseEntity.STATUS_FAIL, ERROR.ErrorCode.THIRD_PARTY_CREDIT, ERROR.ErrorMsg.THIRD_PARTY_CREDIT, null);
        }
        // 封装数据 进行响应
        Map<String, Object> allData = formateDate(userAndBorrowInfo, jsonObject);
        return new ResponseEntity(ResponseEntity.STATUS_OK, allData);
    }

    private Map<String, Object> formateDate(UserAndBorrowInfo userAndBorrowInfo, JSONObject jsonObject) {
        Map<String, Object> allData = new HashMap<>();
        // 订单ID
        allData.put("nid", userAndBorrowInfo.getNid());
        // 借款金额
        allData.put("amount", userAndBorrowInfo.getAmount());
        // 产品ID
        allData.put("productId", null == userAndBorrowInfo.getProductId() ? 0 : userAndBorrowInfo.getProductId());
        // 申请时间
        allData.put("applyTime", userAndBorrowInfo.getApplyTime());
        // 最后一次借款状态
        allData.put("lastBorrowStatus", userAndBorrowInfo.getLastBorrowStatus() == null ? "" : userAndBorrowInfo.getLastBorrowStatus());
        // 手机号
        allData.put("username", userAndBorrowInfo.getUserName());
        // 成功次数
        allData.put("successCount", userAndBorrowInfo.getSuccessCount() == null ? "null" : userAndBorrowInfo.getSuccessCount());
        // 最大逾期天数
        allData.put("maxDelinquentDays", userAndBorrowInfo.getMaxDelinquentDays() == null ? "null" : userAndBorrowInfo.getMaxDelinquentDays());
        // 身份证号码
        allData.put("cardId", userAndBorrowInfo.getCardId());
        // 第一联系人手机号
        allData.put("mainContact1Phone", userAndBorrowInfo.getMainContact1Phone());
        // 第二联系人手机号
        allData.put("mainContact2Phone", userAndBorrowInfo.getMainContact2Phone());
        // 第一联系人关系
        allData.put("mainContact1Relation", relationMap.get(userAndBorrowInfo.getMainContact1Relation()) == null ? "" : relationMap.get(userAndBorrowInfo.getMainContact1Relation()));
        // 第二联系人关系
        allData.put("mainContact2Relation", relationMap.get(userAndBorrowInfo.getMainContact2Relation()) == null ? "" : relationMap.get(userAndBorrowInfo.getMainContact2Relation()));
        // 前海好信分
        allData.put("credooScore", jsonObject.containsKey("credooScore") ? jsonObject.getInteger("credooScore") : "null");
        // 前海常贷客
        allData.put("qianhaiOftenloan", jsonObject.containsKey("qianhaiOftenloan") ? jsonObject.getJSONArray("qianhaiOftenloan") : "null");
        // 前海风险度
        allData.put("qh_riskmark", jsonObject.containsKey("qh_riskmark") ? jsonObject.getJSONArray("qh_riskmark") : "null");
        // 同盾风险
        allData.put("risk_items", jsonObject.containsKey("risk_items") ? jsonObject.getJSONArray("risk_items") : "null");

        // 获取聚信立通话记录
        MongoQuery query = new MongoQuery("phone", userAndBorrowInfo.getUserName(), MongoQuery.MongoQueryBaseType.eq);
        List<MongoQuery> queries = new ArrayList<>();
        queries.add(query);
        JSONObject jxlData = mongoDao.findOne(queries, JSONObject.class, "mobile_raw");
        String gatherTime = null;
        Map<String, Object> retMap = new HashMap<>();
        if (jxlData != null) {
            // 解析数据
            JSONObject rawDataJsonObject = jxlData.getJSONObject("raw_data");
            if (null == rawDataJsonObject) {
                JSONObject data = jxlData.getJSONObject("data");
                if (null != data) {
                    rawDataJsonObject = data.getJSONObject("raw_data");
                }
            }

            if (null != rawDataJsonObject && null != rawDataJsonObject.getJSONObject("members")
                    && ((rawDataJsonObject.getJSONObject("members")).getJSONArray("transactions")).size() > 0) {
                List<JXLCallRecordsResponse> list = new ArrayList<>();
                JSONObject membersJsonObject = rawDataJsonObject.getJSONObject("members");
                if (StringUtils.isNotBlank(membersJsonObject.getString("update_time"))) {
                    gatherTime = membersJsonObject.getString("update_time");
                }
                JSONArray transactionsIsonArray = membersJsonObject.getJSONArray("transactions");
                JSONObject transactionsJsonObject = transactionsIsonArray.getJSONObject(0);
                JSONArray callsJsonArray = transactionsJsonObject.getJSONArray("calls");
                if (!CollectionUtils.isEmpty(callsJsonArray)) {
                    for (Object object : callsJsonArray) {
                        JSONObject jxl = (JSONObject) object;
                        JXLCallRecordsResponse jxlCallRecordsResponse = new JXLCallRecordsResponse();
                        jxlCallRecordsResponse.setUpdateTime(null == jxl.get("update_time") ? "null" : jxl.getString("update_time"));
                        jxlCallRecordsResponse.setStartTime(null == jxl.get("start_time") ? "null" : jxl.getString("start_time"));
                        jxlCallRecordsResponse.setInitType(StringUtils.isBlank(jxl.getString("init_type")) ? "null" : jxl.getString("init_type"));
                        jxlCallRecordsResponse.setUseTime(null == jxl.get("use_time") ? -1 : Double.valueOf(jxl.getString("use_time")).longValue());
                        jxlCallRecordsResponse.setPlace(null == jxl.get("place") ? "null" : jxl.getString("place"));
                        jxlCallRecordsResponse.setOtherCellPhone(StringUtils.isBlank(jxl.getString("other_cell_phone")) ? "null" : jxl.getString("other_cell_phone"));
                        jxlCallRecordsResponse.setCellPhone(null == jxl.get("cell_phone") ? "null" : jxl.getString("cell_phone"));
                        jxlCallRecordsResponse.setSubtotal(null == jxl.get("subtotal") ? -1 : Double.valueOf(jxl.getString("subtotal")).longValue());
                        jxlCallRecordsResponse.setCallType(StringUtils.isBlank(jxl.getString("call_type")) ? "null" : jxl.getString("call_type"));
                        list.add(jxlCallRecordsResponse);
                    }
                    retMap.put("call_records", list);
                } else {
                    retMap.put("call_records", "");
                }
            }
        }
        // 聚信爬取时间
        allData.put("gatherTime", gatherTime);
        allData.put("jxl_call_records", retMap);

        allData.put("isRepay", "");
        List list = new ArrayList();
        list.add("null");
        list.add("null");
        allData.put("contractBeginEndDate", list);
        allData.put("isClean", "");
        allData.put("clearTime", "");
        allData.put("totalRepayAmount", "");
        allData.put("shouldRepayAmount", "");
        allData.put("delinquentDays", "");

        // 新颜-申请雷达
        JSONObject xinyanApplyJson = this.getXinyanData(userAndBorrowInfo.getName(), userAndBorrowInfo.getUserName(), userAndBorrowInfo.getCardId(), "1", 1);
        allData.put("xinyanApply", null == xinyanApplyJson ? "" : xinyanApplyJson);

        // 新颜-行为雷达
        JSONObject xinyanBehaviorJson = this.getXinyanData(userAndBorrowInfo.getName(), userAndBorrowInfo.getUserName(), userAndBorrowInfo.getCardId(), "1", 2);
        allData.put("xinyanBehavior", null == xinyanBehaviorJson ? "" : xinyanBehaviorJson);

        return allData;
    }

    /**
     * 查询 新颜-申请雷达、行为雷达 数据
     *
     * @param name   姓名
     * @param mobile 电话
     * @param idcard 身份证
     * @param expire 数据过期校验，其他：不校验，1：校验
     * @param type   1:新颜-申请雷达  2:新颜-行为雷达
     * @return
     */
    private JSONObject getXinyanData(String name, String mobile, String idcard, String expire, Integer type) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(idcard) || null == type) {
            return null;
        }
        String url = "";
        if (1 == type) {
            url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "risk-lead.xinyan.apply.url");
        } else {
            url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "risk-lead.xinyan.behavior.url");
        }

        Map params = new HashMap<>();
        params.put("idName", name);
        params.put("phoneNo", mobile);
        params.put("idNo", idcard);
        params.put("expire", expire);
        Map<String, String> header = new HashMap<>();

        HttpClientUtils.setHeader(header);
        try {
            ResponseEntity str = dataBaseUtils.doPostRiskLead(url, JSONObject.toJSONString(params));
            if (null != str && "1".equals(str.getStatus()) && null != str.getData()) {
                return JSONObject.parseObject(JSONObject.toJSONString(str.getData()));
            }
        } catch (Throwable e) {
            log.error("查询新颜雷达数据异常,params:{},url:{},error:{}", new Object[]{params, url, e});
        }
        return null;
    }

    /**
     * 调用秒白条接口查询对应的数据
     *
     * @param userAndBorrowInfo
     * @return
     */
    private JSONObject getThirdPartyCredit(UserAndBorrowInfo userAndBorrowInfo) {
        JSONObject result = new JSONObject();
        String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "miaobt.getThirdPartyCredit.url");
        String uid = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.SYSTEMCFG, "rcs.uid");
        String pwkey = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.SYSTEMCFG, "rcs.pwkey");
        Map<String, String> header = new HashMap<>();
        header.put("Content-uid", uid);
        header.put("Content-pwkey", pwkey);
        HttpClientUtils.setHeader(header);

        Map<String, String> params = new HashMap<>();
        params.put("phone", userAndBorrowInfo.getUserName());
        params.put("cardId", userAndBorrowInfo.getCardId());
        params.put("name", userAndBorrowInfo.getName());

        params.put("nid", userAndBorrowInfo.getNid());
        params.put("applyTime", userAndBorrowInfo.getApplyTime() == null ? "" : userAndBorrowInfo.getApplyTime().toString());
        params.put("userBankNo", userAndBorrowInfo.getUserBankNo());
        log.debug("getThirdPartyCredit request params:{},url:{}", params, url);
        String resultStr = "";
        try {
            resultStr = HttpClientUtils.doPost(url, params);
            log.debug("getThirdPartyCredit return:{}", resultStr);
        } catch (Throwable e) {
            log.error("getThirdPartyCredit request params data：{}", params);
            log.error("getThirdPartyCredit 地址：{},查询秒白条信息异常{}", url, e);
        }
        // 第三方结果处理
        if (StringUtils.isNotBlank(resultStr)) {
            // 结果解析
            JSONObject tmp = JSONObject.parseObject(resultStr);
            if (ResponseEntity.STATUS_OK.equals(tmp.getString("status"))) {
                if (tmp.containsKey("data")) {
                    result = tmp.getJSONObject("data");
                }
            }
        } else {
            return null;
        }
        return result;
    }

    /**
     * 调saas接口查询用户信息
     * 根据借款nid 查询用户信息和借款信息
     *
     * @param nid
     * @return
     */
    private UserAndBorrowInfo getUserAndBorrowInfoByNid(String nid) {
        String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "saas.getUserAndBorrowInfoByNid.url");
        Map<String, String> params = new HashMap<>();
        params.put("nid", nid);
        log.debug("getUserAndBorrowInfoByNid request params:{},url:{}", params, url);
        String resultStr = "";
        try {
            resultStr = HttpClientUtils.doGet(url, params);
            log.debug("getUserAndBorrowInfoByNid return:{}", resultStr);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        UserAndBorrowInfo userAndBorrowInfo = null;
        if (StringUtils.isNotBlank(resultStr)) {
            try {
                ResponseEntity rs = new ObjectMapper().readValue(resultStr, ResponseEntity.class);
                if (ResponseEntity.STATUS_OK.equals(rs.getStatus()) && rs.getData() != null) {
                    String jsonStr = MAPPER.writeValueAsString(rs.getData());
                    if (StringUtils.isNotBlank(jsonStr)) {
                        userAndBorrowInfo = MAPPER.readValue(jsonStr, UserAndBorrowInfo.class);
                    }
                }
            } catch (IOException e) {
                log.error("getUserAndBorrowInfoByNid request params data：{}", params);
                log.error("getUserAndBorrowInfoByNid 地址：{},查询用户和借款信息异常{}", url, e);
            }
        }
        return userAndBorrowInfo;
    }

    /**
     * 通知业务系统风控结果
     *
     * @param nid
     * @param admResult
     * @return
     */
    public ResponseEntity noticeBorrowResultHandle(String nid, AdmissionResultDTO admResult) {

        log.info("回调通知入参：nid:{},result:{},RejectReasion:{}", nid, admResult.getResult(), admResult.getRejectReason());
        String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG, "credit.decision.callback.url");
        Map<String, Object> params = new HashMap<>();
        params.put("nid", nid);

        // 不等于通过，人工审核，直接拒绝
        if (admResult.getResult() != AdmissionResultDTO.RESULT_APPROVED
                && admResult.getResult() != AdmissionResultDTO.RESULT_MANUAL) {

            admResult.setResult(AdmissionResultDTO.RESULT_REJECTED);
        }

        String rejectReasons = admResult.getRejectReason() == null ? "" : StringUtils.join(admResult.getRejectReason(), ",");
        Integer controlDays = this.getMaxCloseDays(admResult.getRejectReason());

        AdmissionResultDetail detail = admissionResultDetailDao.getLastDetailByResultId(nid);
        params.put("status", String.valueOf(admResult.getResult()));
        params.put("rejectReasons", rejectReasons);
        params.put("controlDays", null == controlDays ? "" : String.valueOf(controlDays));
        params.put("msg", null == detail || null == detail.getData() ? "" : detail.getData());

        String resultStr = null;
        String msg = null;
        try {
            resultStr = HttpClientUtils.doPost(url, JSONObject.toJSONString(params), "application/json");
        } catch (Throwable e) {
            if (null != e) {
                msg = e.getMessage();
                if (StringUtils.isNotBlank(msg) && msg.length() >= 4000) {
                    msg = msg.substring(0, 3999);
                }
            }
        }
        this.saveResult(nid, admResult, resultStr, msg);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    /**
     * 保存回调结果
     *
     * @param nid       订单号
     * @param admResult 决策结果
     * @param resultStr 业务系统回调结果
     * @param msg       异常信息
     */
    private void saveResult(String nid, AdmissionResultDTO admResult, String resultStr, String msg) {
        try {
            JSONObject result = JSONObject.parseObject(resultStr);
            if (null != result && result.containsKey("code") && "0".equals(result.getString("code"))) {
                saveErrorNoticeSaas(nid, msg, admResult, 1);
            } else {
                saveErrorNoticeSaas(nid, msg, admResult, 2);
            }
        } catch (Exception e) {
            log.error("决策回调通知保存回调处理结果异常：{}", e);
        }
    }

    /**
     * 查询最大拒绝天数
     *
     * @param rejectReason 拒绝原因列表
     * @return
     */
    private Integer getMaxCloseDays(Set<String> rejectReason) {
        try {
            if (null != rejectReason && rejectReason.size() > 0) {
                return rejectReasonDao.getMaxCloseDays(rejectReason);
            }
        } catch (Exception e) {
            log.error("通过拒绝原因查询管制天数失败rejectReason：{},error", rejectReason, e);
        }
        return null;
    }

    /**
     * 通知业务系统风控结果
     *
     * @param nid
     * @param noticeNum
     * @return
     */
    @Override
    public ResponseEntity pushRiskResult(String nid, Integer noticeNum) {
        if (noticeNum == null) {
            noticeNum = 5;
        }
        // 查询通知失败的记录
        List<DecisionResultNotice> noticeList = decisionResultNoticeDao.pushRiskResult(nid, noticeNum);
        if (null != noticeList && noticeList.size() > 0) {
            for (DecisionResultNotice notice : noticeList) {
                try {
                    AdmissionResultDTO admResult = new AdmissionResultDTO();
                    admResult.setResult(notice.getResult());
                    // 失败原因
                    if (StringUtils.isNotBlank(notice.getRejectReasons())) {
                        Set<String> rejectReason = new HashSet<>(Arrays.asList(notice.getRejectReasons().split(",")));
                        admResult.setRejectReason(rejectReason);
                    }
                    noticeBorrowResultHandle(notice.getNid(), admResult);
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }

    /**
     * 通知业务系统风控结果
     *
     * @return
     */
    @Scheduled(cron = "0 0/10 * * * ? ")
    public ResponseEntity pushRiskResult() {
        log.warn("定时任务开始：重新推送风控结果");
        this.pushRiskResult(null, 5);
        return new ResponseEntity(ResponseEntity.STATUS_OK);
    }


    /**
     * 通知saas记录信息
     *
     * @param nid
     * @param admResult
     * @param status
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.DEFAULT, timeout = 36000, rollbackFor = Exception.class)
    public void saveErrorNoticeSaas(String nid, String msg, AdmissionResultDTO admResult, Integer status) {
        DecisionResultNotice saasNotice = decisionResultNoticeDao.getByNid(nid);
        try {
            if (saasNotice != null && saasNotice.getStatus() != 1) {
                saasNotice.setStatus(status);
                saasNotice.setResult(admResult.getResult());
                saasNotice.setRejectReasons(admResult.getRejectReason() == null ? "" : StringUtils.join(admResult.getRejectReason(), ","));
                saasNotice.setNoticeNum(saasNotice.getNoticeNum() + 1);
                saasNotice.setUpdateTime(System.currentTimeMillis());
                saasNotice.setMsg(msg);
                int i = decisionResultNoticeDao.updateByPrimaryKeySelective(saasNotice);
            } else if (saasNotice == null) {
                saasNotice = new DecisionResultNotice();
                saasNotice.setStatus(status);
                saasNotice.setResult(admResult.getResult());
                saasNotice.setNoticeNum(1);
                saasNotice.setNid(nid);
                saasNotice.setRejectReasons(admResult.getRejectReason() == null ? "" : StringUtils.join(admResult.getRejectReason(), ","));
                saasNotice.setAddTime(System.currentTimeMillis());
                saasNotice.setMsg(msg);
                int i = decisionResultNoticeDao.insert(saasNotice);
            }
        } catch (Exception e) {
            log.error("通知更新异常：{}", e);
        }
    }

    /**
     * 模型异常处理
     *
     * @param request
     * @return
     */
    public Integer robotNoticeError(RobotResultRequest request) {
        DecisionRobotNotice findNotice = new DecisionRobotNotice();
        findNotice.setNid(request.getNid());
        DecisionRobotNotice notice = decisionRobotNoticeDao.getByNid(findNotice);
        try {
            // 判断模型结果如果是2：异常
            if (Integer.compare(request.getStatus(), 2) == 0) {
                // 如果有异常则记录
                if (notice == null) {
                    notice = new DecisionRobotNotice();
                    notice.setNid(request.getNid());
                    notice.setStatus(1);
                    notice.setNoticeNum(1);
                    notice.setExptStatus(1);
                    notice.setAddUser(0L);
                    notice.setAddTime(System.currentTimeMillis());
                    decisionRobotNoticeDao.insertSelective(notice);
                    return 1;
                } else {
                    // 如果状态完成则不处理
                    if (Integer.compare(2, notice.getExptStatus()) == 0) {
                        log.debug("nid：{}", request.getNid());
                        return 1;
                    }
                    // 其他情况
                    if (notice.getNoticeNum() < 4) {
                        return 1;
                    } else {
                        // 超过5次
                        notice.setStatus(1);
                        notice.setUpdateTime(System.currentTimeMillis());
                        notice.setExptStatus(2); // 完成
                        decisionRobotNoticeDao.updateByPrimaryKeySelective(notice);
                        return 2;
                    }
                }
            }
            // 处理通知
            if (Integer.compare(request.getStatus(), 1) == 0) {
                if (notice != null) {
                    notice.setStatus(1);
                    notice.setExptStatus(2); //完成
                    notice.setUpdateTime(System.currentTimeMillis());
                    decisionRobotNoticeDao.updateByPrimaryKeySelective(notice);
                    return 3;
                }
            }
        } catch (Exception e) {
            log.error("nid：{}处理模型通知异常：{}", request.getNid(), e);
        }
        return 3;
    }
}

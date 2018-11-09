package com.risk.controller.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.utils.IdcardUtils;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.RiskModelOperatorReportDao;
import com.risk.controller.service.dao.StaOperatorCallsDao;
import com.risk.controller.service.dao.StaUserBaseinfoDao;
import com.risk.controller.service.entity.DataOrderMapping;
import com.risk.controller.service.handler.MongoHandler;
import com.risk.controller.service.mongo.dao.MongoCollections;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.DataOrderMappingService;
import com.risk.controller.service.service.ModelDataService;
import com.risk.controller.service.service.PaixuService;
import com.risk.controller.service.utils.Average;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 保存模型数据的service
 */
@Slf4j
@Service
public class ModelDataServiceImpl implements ModelDataService {

    @Autowired
    private MongoHandler mongoHandler;
    @Autowired
    private StaOperatorCallsDao staOperatorCallsDao;
    @Autowired
    private StaUserBaseinfoDao staUserBaseinfoDao;
    @Autowired
    private RiskModelOperatorReportDao riskModelOperatorReportDao;
    @Autowired
    private DataOrderMappingService dataOrderMappingService;
    @Autowired
    private PaixuService paixuService;

    /**
     * 入口方法
     *
     * @param request
     */
    @Override
    public void saveData(DecisionHandleRequest request) throws Exception {
        this.saveOperatorCalls(request);
    }

    /**
     * 保存运营商通话记录
     *
     * @param request
     * @throws Exception
     */
    private void saveOperatorCalls(DecisionHandleRequest request) throws Exception {

    }

    /**
     * 获取运营商报告信息
     *
     * @param request
     * @return
     */
    public JSONObject getOperatorReport(DecisionHandleRequest request) {
        JSONObject report = null;

        if (null != request.getRobotRequestDTO().getOperatorReport()) {
            return request.getRobotRequestDTO().getOperatorReport();
        }

        DataOrderMapping dataOrderMapping = dataOrderMappingService.getLastOneByNid(request.getNid());
        Map<String, Object> queryMap = new HashMap<String, Object>();
        queryMap.put("number", dataOrderMapping.getOperatorNum());

        List<JSONObject> rawReport = paixuService.getEqMongoData(queryMap, MongoCollections.DB_OPERATOR_MOJIE_INFO);
        if (null != rawReport && rawReport.size() > 0) {
            JSONObject object = (JSONObject) rawReport.get(0);
            report = JSON.parseObject(object.getString("clientReport"), JSONObject.class);
            request.getRobotRequestDTO().setOperatorReport(report);
        } else {
            return null;
        }

        return report;
    }

    /**
     * 风险通话分析保存
     * @param request
     */
    public void genCallRiskAnalysis (DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);
        JSONArray riskCalls = operatorReport.getJSONArray("call_risk_analysis");

        riskCalls.forEach( call -> {
            JSONObject call_risk_analysis = (JSONObject) call;
            JSONObject analysis_point =  ((JSONObject) call).getJSONObject("analysis_point");
            JSONObject call_analysis_dial_point =  analysis_point.getJSONObject("call_analysis_dial_point");
            JSONObject call_analysis_dialed_point =  analysis_point.getJSONObject("call_analysis_dialed_point");

            Map params = new HashMap();
            params.put("nid", request.getNid());
            params.put("phone", request.getUserName());
            params.put("analysis_item",call_risk_analysis.getString("analysis_item"));
            params.put("analysis_desc",call_risk_analysis.getString("analysis_desc"));

            params.put("avg_call_time_6m",analysis_point.getString("avg_call_time_6m"));
            params.put("call_cnt_1m",analysis_point.getString("call_cnt_1m"));
            params.put("call_cnt_3m",analysis_point.getString("call_cnt_3m"));
            params.put("call_cnt_6m",analysis_point.getString("call_cnt_6m"));
            params.put("call_time_3m",analysis_point.getString("call_time_3m"));
            params.put("avg_call_cnt_6m",analysis_point.getString("avg_call_cnt_6m"));
            params.put("call_time_6m",analysis_point.getString("call_time_6m"));
            params.put("avg_call_cnt_3m",analysis_point.getString("avg_call_cnt_3m"));
            params.put("call_time_1m",analysis_point.getString("call_time_1m"));
            params.put("avg_call_time_3m",analysis_point.getString("avg_call_time_3m"));

            params.put("call_dial_time_6m",call_analysis_dial_point.getString("call_dial_time_6m"));
            params.put("call_dial_time_3m",call_analysis_dial_point.getString("call_dial_time_3m"));
            params.put("avg_call_dial_cnt_6m",call_analysis_dial_point.getString("avg_call_dial_cnt_6m"));
            params.put("call_dial_cnt_1m",call_analysis_dial_point.getString("call_dial_cnt_1m"));
            params.put("avg_call_dial_cnt_3m",call_analysis_dial_point.getString("avg_call_dial_cnt_3m"));
            params.put("avg_call_dial_time_6m",call_analysis_dial_point.getString("avg_call_dial_time_6m"));
            params.put("call_dial_cnt_3m",call_analysis_dial_point.getString("call_dial_cnt_3m"));
            params.put("avg_call_dial_time_3m",call_analysis_dial_point.getString("avg_call_dial_time_3m"));
            params.put("call_dial_time_1m",call_analysis_dial_point.getString("call_dial_time_1m"));
            params.put("call_dial_cnt_6m",call_analysis_dial_point.getString("call_dial_cnt_6m"));


            params.put("call_dialed_time_6m",call_analysis_dialed_point.getString("call_dialed_time_6m"));
            params.put("call_dialed_time_3m",call_analysis_dialed_point.getString("call_dialed_time_3m"));
            params.put("avg_call_dialed_cnt_6m",call_analysis_dialed_point.getString("avg_call_dialed_cnt_6m"));
            params.put("call_dialed_cnt_1m",call_analysis_dialed_point.getString("call_dialed_cnt_1m"));
            params.put("avg_call_dialed_cnt_3m",call_analysis_dialed_point.getString("avg_call_dialed_cnt_3m"));
            params.put("avg_call_dialed_time_6m",call_analysis_dialed_point.getString("avg_call_dialed_time_6m"));
            params.put("call_dialed_cnt_3m",call_analysis_dialed_point.getString("call_dialed_cnt_3m"));
            params.put("avg_call_dialed_time_3m",call_analysis_dialed_point.getString("avg_call_dialed_time_3m"));
            params.put("call_dialed_time_1m",call_analysis_dialed_point.getString("call_dialed_time_1m"));
            params.put("call_dialed_cnt_6m",call_analysis_dialed_point.getString("call_dialed_cnt_6m"));

            riskModelOperatorReportDao.genCallRiskAnalysis(params);

        });
    }

    /**
     * 信息校验数据保存
     * @param request
     */
    public void genBasicCheckItem (DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);

        JSONArray basic_check_items = operatorReport.getJSONArray("basic_check_items");
        Map params = new HashMap();
        params.put("nid", request.getNid());
        params.put("phone", request.getUserName());

        basic_check_items.forEach( call -> {
            JSONObject item = (JSONObject) call;

            String check_item = item.getString("check_item") == null ? "" : item.getString("check_item").equalsIgnoreCase("null") ? "" : item.getString("check_item");
            if (StringUtils.isEmpty(check_item)) {
                return;
            }

            String comment = item.getString("comment") == null ? "" : item.getString("comment").equalsIgnoreCase("null") ? "" : item.getString("comment");
            switch (check_item) {
                case "idcard_check" :
                    params.put("idcard_check", item.getString("result"));
                    params.put("idcard_check_comment", comment);
                    break;
                case "email_check" :
                    params.put("email_check", item.getString("result"));
                    params.put("email_check_comment", comment);
                    break;
                case "address_check" :
                    params.put("address_check", item.getString("result"));
                    params.put("address_check_comment", comment);
                    break;
                case "call_data_check" :
                    params.put("call_data_check", item.getString("result"));
                    params.put("call_data_check_comment", comment);
                    break;
                case "idcard_match" :
                    params.put("idcard_match", item.getString("result"));
                    params.put("idcard_match_comment", comment);
                    break;
                case "name_match" :
                    params.put("name_match", item.getString("result"));
                    params.put("name_match_comment", comment);
                    break;
                case "is_name_and_idcard_in_court_black" :
                    params.put("is_name_and_idcard_in_court_black", item.getString("result"));
                    params.put("is_name_and_idcard_in_court_black_comment", comment);
                    break;
                case "is_name_and_idcard_in_finance_black" :
                    params.put("is_name_and_idcard_in_finance_black", item.getString("result"));
                    params.put("is_name_and_idcard_in_finance_black_comment", comment);
                    break;
                case "is_name_and_mobile_in_finance_black" :
                    params.put("is_name_and_mobile_in_finance_black", item.getString("result"));
                    params.put("is_name_and_mobile_in_finance_black_comment", comment);
                    break;
                case "mobile_silence_3m" :
                    params.put("mobile_silence_3m", item.getString("result"));
                    params.put("mobile_silence_3m_comment", comment);
                    break;
                case "mobile_silence_6m" :
                    params.put("mobile_silence_6m", item.getString("result"));
                    params.put("mobile_silence_6m_comment", comment);
                    break;
                case "arrearage_risk_3m" :
                    params.put("arrearage_risk_3m", item.getString("result"));
                    params.put("arrearage_risk_3m_comment", comment);
                    break;
                case "arrearage_risk_6m" :
                    params.put("arrearage_risk_6m", item.getString("result"));
                    params.put("arrearage_risk_6m_comment", comment);
                    break;
                case "binding_risk" :
                    params.put("binding_risk", item.getString("result"));
                    params.put("binding_risk_comment", comment);
                    break;
                default:
                    return;
            }

            riskModelOperatorReportDao.genBasicCheckItem(params);

        });
    }

    /**
     *  用户信息监测
     * @param request
     */
    public void genUserInfoCheck (DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);

        JSONArray user_info_check = operatorReport.getJSONArray("user_info_check");
        user_info_check.forEach( call -> {
            JSONObject item = (JSONObject) call;
            if (null == item || null == item.getJSONObject("check_black_info")) {
                return;
            }

            JSONObject check_black_info = item.getJSONObject("check_black_info");
            check_black_info.put("nid", request.getNid());
            check_black_info.put("phone", request.getUserName());

            JSONObject check_search_info = item.getJSONObject("check_search_info");
            Set<String> searchInfoKeys = check_search_info.keySet();
            searchInfoKeys.forEach(key -> {
                check_black_info.put(key, check_search_info.get(key));
                if (key.equalsIgnoreCase("idcard_with_other_names")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
                if (key.equalsIgnoreCase("idcard_with_other_phones")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
                if (key.equalsIgnoreCase("phone_with_other_names")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
                if (key.equalsIgnoreCase("phone_with_other_idcards")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
                if (key.equalsIgnoreCase("arised_open_web")) {
                    check_black_info.put(key + "_cnt", check_search_info.getJSONArray(key).size());
                }
            });
            Date currDate = new Date();
            check_black_info.put("add_time", currDate);
            check_black_info.put("update_time", currDate);

            riskModelOperatorReportDao.genCheckBlackInfo(check_black_info);
        });
    }

    /**
     * 亲情网相关
     * @param request
     */
    public void genCallFamilyDetail (DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);

        JSONArray call_family_detail = operatorReport.getJSONArray("call_family_detail");
        call_family_detail.forEach( call -> {
            JSONObject detail = (JSONObject) call;
            JSONObject item = detail.getJSONObject("item");
            item.put("nid", request.getNid());
            item.put("phone", request.getUserName());
            item.put("app_point", detail.getString("app_point"));
            item.put("app_point_zh", detail.getString("app_point_zh"));
            riskModelOperatorReportDao.genCallFamilyDetail(item);
        });
    }

    /**
     * 通话时段分析-深夜通话
     * @param request
     */
    public void genCallMidnight (DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);
        JSONArray call_duration_detail = operatorReport.getJSONArray("call_duration_detail");
        call_duration_detail.forEach( call -> {
            JSONObject detail = (JSONObject) call;
            if (detail.getString("key").equalsIgnoreCase("call_duration_detail_3m")) {
                JSONArray duration_list = detail.getJSONArray("duration_list");
                duration_list.forEach(item -> {
                    JSONObject itemJson = (JSONObject) item;
                    if (itemJson.getString("time_step").equals("midnight")) {
                        itemJson = itemJson.getJSONObject("item");
                        itemJson.put("nid", request.getNid());
                        itemJson.put("phone", request.getUserName());
                        itemJson.put("time_step", "midnight");
                        itemJson.put("time_step_zh", "深夜[1:30-5:30]");

                        riskModelOperatorReportDao.genCallMidnight(itemJson);
                    }
                });
            }
        });
    }

    /**
     * 手机静默-联系人-出行-月平均话费信息
     * @param request
     */
    public void genCallSilentAreasBill (DecisionHandleRequest request) {
        JSONObject operatorReport = this.getOperatorReport(request);

        JSONObject params = new JSONObject();
        JSONArray behaviorCheck = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.BEHAVIOR_CHECK.getValue());
        // 平均话费计算
        for (Object item : behaviorCheck) {
            JSONObject itemJson = (JSONObject) item;
            if (StringUtils.isNotEmpty(itemJson.getString("check_point"))
                    && !itemJson.getString("check_point").equalsIgnoreCase("phone_silent")) {
                params.put("score", itemJson.getInteger("score"));
            }
        }
        // 平均话费
        JSONArray bills = operatorReport.getJSONArray("bills");
        List<Integer> arr = new ArrayList<>();
        if (null != bills && !bills.isEmpty()) {
            for (Object bill : bills) {
                JSONObject itemJson = (JSONObject) bill;
                arr.add(((JSONObject) bill).getIntValue("actual_fee"));
            }
            Collections.sort(arr);
        }
        params.put("avg_charge", Double.valueOf(Average.getAverages(arr)));
        // 出行
        JSONArray tripInfo = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.TRIP_INFO.getValue());
        Set<String> areas = new HashSet<>();
        for (Object item : tripInfo) {
            JSONObject itemJson = (JSONObject) item;
            if (StringUtils.isNotEmpty(itemJson.getString("trip_dest"))
                    && !itemJson.getString("trip_dest").equalsIgnoreCase("null")) {
                areas.add(itemJson.getString("trip_dest"));
            }
            if (StringUtils.isNotEmpty(itemJson.getString("trip_leave"))
                    && !itemJson.getString("trip_leave").equalsIgnoreCase("null")) {
                areas.add(itemJson.getString("trip_leave"));
            }
        }
        params.put("trip_cnt", areas.size());
        // 联系人所在区域个数
        JSONArray contactRegion = operatorReport.getJSONArray(MongoCollections.OPERATOR_MOJIE_INFO_ELEMENT.CONTACT_REGION.getValue());
        for (Object item : contactRegion) {
            JSONObject itemJson = (JSONObject) item;
            String key = itemJson.getString("key");
            if (null == key && key.equals("contact_region_6m")) {
                params.put("region_cnt", itemJson.getJSONArray("region_list").size());
            }
        }
        riskModelOperatorReportDao.genCallSilentAreasBill(params);
    }

}

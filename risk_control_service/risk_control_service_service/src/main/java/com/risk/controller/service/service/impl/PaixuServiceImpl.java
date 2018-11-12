package com.risk.controller.service.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.constans.ERROR;
import com.risk.controller.service.common.utils.IdcardUtils;
import com.risk.controller.service.common.utils.ResponseEntity;
import com.risk.controller.service.dao.RiskAreaDao;
import com.risk.controller.service.dao.RiskDecisionReqLogDao;
import com.risk.controller.service.dao.RiskPaixuLogDao;
import com.risk.controller.service.entity.DataOrderMapping;
import com.risk.controller.service.entity.RiskArea;
import com.risk.controller.service.entity.RiskDecisionReqLog;
import com.risk.controller.service.entity.RiskPaixuLog;
import com.risk.controller.service.enums.CacheCfgType;
import com.risk.controller.service.enums.GetCacheModel;
import com.risk.controller.service.mongo.dao.MongoCollections;
import com.risk.controller.service.mongo.dao.MongoDao;
import com.risk.controller.service.mongo.dao.MongoQuery;
import com.risk.controller.service.request.DecisionHandleRequest;
import com.risk.controller.service.service.PaixuService;
import com.risk.controller.service.utils.paixu.GetLocation;
import com.risk.controller.service.utils.paixu.LocalHttpClient;
import com.risk.controller.service.utils.paixu.SignUtils;
import com.risk.controller.service.utils.paixu.entity.PreApplyWithCreditVO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PaixuServiceImpl implements PaixuService {

	@Autowired
	private LocalCache localCache;

	@Autowired
	private MongoDao mongoDao;

	@Autowired
	private RiskPaixuLogDao riskPaixuLogDao;

	@Autowired
	private RiskAreaDao riskAreaDao;

	@Autowired
	private RiskDecisionReqLogDao riskDecisionReqLogDao;

	@Autowired
	private DataOrderMappingServiceImpl dataOrderMappingService;

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");

	/**
	 * 调用排序
	 * 
	 * @param request
	 * @return
	 */
	@Override
	public ResponseEntity requestPaixu(DecisionHandleRequest request) {
		log.info("[排序请求方法开始，方法入参]：{}", JSON.toJSONString(request));

		RiskPaixuLog paixuLog = riskPaixuLogDao.getPaixu(request.getNid());
		if (null != paixuLog && StringUtils.isNotBlank(paixuLog.getRepParam()) && "000000".equals(paixuLog.getRspCode())) {
			log.info("[排序响应结果-查询本地]：nid:{}", request.getNid());
			return new ResponseEntity(ResponseEntity.STATUS_OK, null, null, JSONObject.parseObject(paixuLog.getRepParam()));
		}

		if (StringUtils.isEmpty(request.getCardId()) || StringUtils.isEmpty(request.getUserName())
				|| StringUtils.isEmpty(request.getNid()) || StringUtils.isEmpty(request.getName())
				|| null == request.getUserId() || null == request.getApplyTime()
//    			|| null == request.getCallRecords()
//    			|| null == request.getSms()
//    			|| null == request.getContacts()
//    			|| null == request.getMainContacts()
//    			|| null == request.getUserCharges()
//    	    	|| null == request.getOperatorReport()

		) {

			log.info("[排序请求方法参数不足]：{}", JSON.toJSONString(request));

			return new ResponseEntity(ResponseEntity.STATUS_FAIL, ERROR.ErrorCode.PARAMS_LACK,
					ERROR.ErrorMsg.PARAMS_LACK, null);
		}



		String merchantId = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.SYSTEMCFG,
				"atm.paixu.assign.merchantId");
		String productId = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.SYSTEMCFG,
				"atm.paixu.assign.productId");
		String privateKey = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.SYSTEMCFG,
				"atm.rsa.private.key");
		String url = localCache.getLocalCache(GetCacheModel.NO_FLUSH, CacheCfgType.THIRDSERVICECFG,
				"atm.paixu.apply.url");

		PreApplyWithCreditVO pre = new PreApplyWithCreditVO();
		pre.setCardNum(request.getCardId());
		pre.setMobile(request.getUserName());
		pre.setOrderId(request.getNid());
		pre.setMerchantId(merchantId);
		pre.setTimeStamp(DateUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss"));
		pre.setRiskData(new JSONObject());
		pre.setProductId(productId);
		pre.setUserName(request.getName());
		pre.setSign(SignUtils.getSign(pre, privateKey));

		ResponseEntity riskDataEntity = this.getRiskData(request);
		if (null != riskDataEntity && riskDataEntity.getStatus().equals(ResponseEntity.STATUS_OK)) {
			pre.setRiskData((JSONObject) riskDataEntity.getData());
		} else {
			return riskDataEntity;
		}

		log.debug("[{}请求排序参数]：{}", request.getNid(), JSON.toJSONString(pre));

		// 插入请求日志开始
		long startMills = System.currentTimeMillis();

		RiskPaixuLog reqLog = new RiskPaixuLog();
		reqLog.setIdName(request.getName());
		reqLog.setIdNo(request.getCardId());
		// reqLog.setReqParam(null);
		reqLog.setPhone(request.getUserName());
		reqLog.setNid(request.getNid());
		reqLog.setStatus(0);
		reqLog.setReqTime(startMills);
		reqLog.setStatus(0);

		JSONObject result = null;
		try {
//			result = HttpClientUtils.doPost(url, requestParam.toJSONString()); // 发起请求
			result = LocalHttpClient.executePost(url, JSON.toJSONString(pre), JSONObject.class);

			if (null != result) {
				result.remove("sign");
				reqLog.setRepParam(result.toJSONString());
				reqLog.setStatus(1);
				reqLog.setResult(result.getString("result"));
				reqLog.setScore(result.getDouble("score"));
				String errorCode = result.getString("rspCode");
				reqLog.setRspCode(StringUtils.isBlank(errorCode) ? "" : errorCode);
			} else {
				reqLog.setStatus(2);
				reqLog.setRspCode("");
			}

			log.info("[排序响应结果]：{}", result);
		} catch (Exception e) {
			reqLog.setStatus(2);
			reqLog.setRspCode("");
			log.error("[请求排序失败]", e);
		}

		long endMills = System.currentTimeMillis();
		reqLog.setAddTime(endMills);
		reqLog.setUpdateTime(endMills);
		reqLog.setReqCnt(endMills - startMills);

		riskPaixuLogDao.insert(reqLog);

		ResponseEntity responseEntity = null;
		if (null != result) {
			responseEntity = new ResponseEntity(ResponseEntity.STATUS_OK, result);
		} else {
			responseEntity = new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "排序请求异常", null);
		}

		log.info("[排序请求方法返回]：{}", JSON.toJSONString(responseEntity));
		return responseEntity;
	}

	public ResponseEntity getContactTime(String nids) throws ParseException {
		String[] nidArr = nids.split(",");
		List list = new ArrayList<>();
		for (String nid : nidArr) {
			nid = nid.trim();

			DataOrderMapping dataOrderMapping = dataOrderMappingService.getLastOneByNid(nid);

			Map<String, Object> queryMap = new HashMap<String, Object>();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			if (null != dataOrderMapping) {
				queryMap.put("number", dataOrderMapping.getClientNum());
				queryMap.put("clientId", dataOrderMapping.getUserId());
				List<JSONObject> urgContact = this.getEqMongoData(queryMap, MongoCollections.DB_USER_MAIN_CONTACT);

				queryMap.put("number", dataOrderMapping.getClientNum());
				queryMap.put("clientId", dataOrderMapping.getUserId());
				List<JSONObject> callRecords = this.getEqMongoData(queryMap, MongoCollections.DB_OPERATOR_CALLS_DETAIL);
				JSONArray callRecord = new JSONArray();
				if (null != callRecords && callRecords.size() > 0 && null != urgContact && urgContact.size() > 0) {
					JSONObject jsonObject = new JSONObject();
					JSONObject jsonObject2 = new JSONObject();

					log.info("[联系人电话1]:{},[联系人电话2]:{}", urgContact.get(0).getString("contactsPhone"),
							urgContact.get(1).getString("contactsPhone"));

					jsonObject.put("call_time", "1990-01-01 12:00:00"); // "call_time": "1509599164260", //呼叫时间
					jsonObject.put("nid", dataOrderMapping.getNid()); // "call_time": "1509599164260", //呼叫时间

					jsonObject2.put("call_time", "1990-01-01 12:00:00");
					jsonObject2.put("nid", dataOrderMapping.getNid());

					for (JSONObject object : callRecords) {
						jsonObject.put("phone", object.getString("phone"));
						jsonObject.put("clientId", object.getString("clientId"));

						jsonObject2.put("phone", object.getString("phone"));
						jsonObject2.put("clientId", object.getString("clientId"));

						// "call_time": "1509599164260", //呼叫时间
						log.info("[通话人电话:{}]", object.getString("peer_number"));
						String phone1 = urgContact.get(0).getString("contactsPhone").trim().replace("-", "");

						if (phone1.equals(object.getString("peer_number").trim())) {
							if (sdf.parse(jsonObject.getString("call_time")).getTime() < sdf
									.parse(object.getString("time")).getTime()) {
								jsonObject.put("call_time", object.getString("time"));
								jsonObject.put("contactPhone1", object.getString("peer_number"));
								jsonObject.put("duration", object.getString("duration"));
								log.info("[匹配1:{}],[时间:{}]", object.getString("peer_number"), object.getString("time"));

							}
						}
						String phone2 = urgContact.get(1).getString("contactsPhone").trim().replace("-", "");
						if (phone2.equals(object.getString("peer_number").trim())) {
							if (sdf.parse(jsonObject.getString("call_time")).getTime() < sdf
									.parse(object.getString("time")).getTime()

							) {
								jsonObject2.put("call_time", object.getString("time")); // "call_time": "1509599164260",
																						// //呼叫时间
								jsonObject2.put("contactPhone2", object.getString("peer_number"));
								jsonObject2.put("duration", object.getString("duration"));
								log.info("[匹配2:{}],[时间:{}]", object.getString("peer_number"), object.getString("time"));

							}
						}

					}
					callRecord.add(jsonObject);
					callRecord.add(jsonObject2);

				}

				list.add(callRecord);

			}
		}
		return new ResponseEntity(ResponseEntity.STATUS_OK, null, "运营商原始信息为空", list);
	}

	public ResponseEntity getContactCount(String nids) throws ParseException {
		String[] nidArr = nids.split(",");
		List list = new ArrayList<>();
		for (String nid : nidArr) {
			nid = nid.trim();

			DataOrderMapping dataOrderMapping = dataOrderMappingService.getLastOneByNid(nid);
			
			Map<String, Object> queryMap = new HashMap<String, Object>();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			if (null != dataOrderMapping) {
				RiskDecisionReqLog riskDecisionReqLog = riskDecisionReqLogDao.getLastBynid(nid);
				JSONObject reqObject = null;
				if (null != riskDecisionReqLog && StringUtils.isNotEmpty(riskDecisionReqLog.getReqData())) {
					reqObject = JSON.parseObject(riskDecisionReqLog.getReqData());
				}
				
				queryMap.put("number", dataOrderMapping.getClientNum());
				queryMap.put("clientId", dataOrderMapping.getUserId());
				List<JSONObject> urgContact = this.getEqMongoData(queryMap, MongoCollections.DB_USER_MAIN_CONTACT);

				queryMap.put("number", dataOrderMapping.getOperatorNum());
				queryMap.put("clientId", dataOrderMapping.getUserId());
				List<JSONObject> callRecords = this.getEqMongoData(queryMap, MongoCollections.DB_OPERATOR_CALLS_DETAIL);
				JSONArray callRecord = new JSONArray();
				if (null != callRecords && callRecords.size() > 0 && null != urgContact && urgContact.size() > 0) {
					JSONObject jsonObject = new JSONObject(new LinkedHashMap());
					JSONObject jsonObject2 = new JSONObject(new LinkedHashMap());

					log.info("[联系人电话1]:{},[联系人电话2]:{}", urgContact.get(0).getString("contactsPhone"),
							urgContact.get(1).getString("contactsPhone"));
					List list1 = new ArrayList<>();
					List list2 = new ArrayList<>();

					jsonObject.put("call_time", "1990-01-01 12:00:00"); // "call_time": "1509599164260", //呼叫时间
					jsonObject.put("nid", dataOrderMapping.getNid()); // "call_time": "1509599164260", //呼叫时间
//					jsonObject.put("records", list1); // "call_time": "1509599164260", //呼叫时间

					int count1 = 0;

					jsonObject2.put("call_time", "1990-01-01 12:00:00");
					jsonObject2.put("nid", dataOrderMapping.getNid());
//					jsonObject2.put("records", list2); 

					int count2 = 0;

					for (JSONObject object : callRecords) {

						jsonObject.put("phone", object.getString("phone"));
						jsonObject2.put("phone", object.getString("phone"));

						if (null != reqObject) {
							jsonObject.put("clientId", reqObject.getString("name"));
							jsonObject2.put("clientId", reqObject.getString("name"));
						} else {
							jsonObject.put("clientId", object.getString("clientId"));
							jsonObject2.put("clientId", object.getString("clientId"));
						}

						JSONObject objectItem1 = new JSONObject();
						JSONObject objectItem2 = new JSONObject();

						// "call_time": "1509599164260", //呼叫时间
						log.info("[通话人电话:{}]", object.getString("peer_number"));
						String phone1 = urgContact.get(0).getString("contactsPhone").trim().replace("-", "");
						jsonObject.put("contactPhone", phone1);

						if (phone1.equals(object.getString("peer_number").trim())) { // 联系人一
//							if (sdf.parse(jsonObject.getString("call_time")).getTime() < sdf
//									.parse(object.getString("time")).getTime()) {
							jsonObject.put("contactPhone", phone1);

							if (sdf.parse(jsonObject.getString("call_time")).getTime() < sdf
									.parse(object.getString("time")).getTime()) {

								jsonObject.put("call_time", object.getString("time"));
								jsonObject.put("duration", object.getString("duration"));
								log.info("[匹配1:{}],[时间:{}]", object.getString("peer_number"), object.getString("time"));
							}
							count1++;
							jsonObject.put("total", count1);
							if (jsonObject.containsKey(object.getString("bill_month") + "月")) {
								int cou = jsonObject.getInteger(object.getString("bill_month") + "月");
								cou++;
								jsonObject.put(object.getString("bill_month") + "月", cou);
							} else {
								jsonObject.put(object.getString("bill_month") + "月", 1);
							}
							
							if (object.getString("location_type").contains("主叫")) {
								if (null != object.getInteger("duration")) {
									jsonObject.put(object.getString("bill_month") + "主叫时长", object.getIntValue("duration") + jsonObject.getIntValue(object.getString("bill_month") + "主叫时长"));
								}
								

								if (null == jsonObject.getInteger(object.getString("bill_month") + "主叫")) {
									jsonObject.put(object.getString("bill_month") + "主叫", 1);
								} else {
									jsonObject.put(object.getString("bill_month") + "主叫", jsonObject.getInteger(object.getString("bill_month") + "主叫") + 1);
								}
							} else if (object.getString("location_type").contains("被叫")){
								if (null != object.getInteger("duration")) {
									jsonObject.put(object.getString("bill_month") + "被叫时长", object.getIntValue("duration") + jsonObject.getIntValue(object.getString("bill_month") + "被叫时长"));
								}
								
								if (null == jsonObject.getInteger(object.getString("bill_month") + "被叫")) {
									jsonObject.put(object.getString("bill_month") + "被叫", 1);
								} else {
									jsonObject.put(object.getString("bill_month") + "被叫", jsonObject.getInteger(object.getString("bill_month") + "被叫") + 1);
								}
							} else {
								if (null != object.getInteger("duration")) {
									jsonObject.put(object.getString("bill_month") + "未知时长", object.getIntValue("duration") + jsonObject.getIntValue(object.getString("bill_month") + "未知时长"));
								}
								
								if (null == jsonObject.getInteger(object.getString("bill_month") + "未知")) {
									jsonObject.put(object.getString("bill_month") + "未知", 1);
								} else {
									jsonObject.put(object.getString("bill_month") + "未知", jsonObject.getInteger(object.getString("bill_month") + "未知") + 1);
								}
							}

							objectItem1.put("phone", object.getString("phone"));
							objectItem1.put("contactPhone", object.getString("peer_number"));
							objectItem1.put("bill_month", object.getString("bill_month"));
							objectItem1.put("location_type", object.getString("location_type"));
							objectItem1.put("time", object.getString("time"));

							list1.add(objectItem1);

						}
						String phone2 = urgContact.get(1).getString("contactsPhone").trim().replace("-", "");
						
						jsonObject2.put("contactPhone", phone2);
						if (phone2.equals(object.getString("peer_number").trim())) { // 联系人
							if (sdf.parse(jsonObject2.getString("call_time")).getTime() < sdf
									.parse(object.getString("time")).getTime()) {
								jsonObject2.put("contactPhone", phone2);

								jsonObject2.put("call_time", object.getString("time")); // "call_time": "1509599164260",
																						// //呼叫时间
								jsonObject2.put("duration", object.getString("duration"));
								log.info("[匹配2:{}],[时间:{}]", object.getString("peer_number"), object.getString("time"));

							}
							
							count2++;
							jsonObject2.put("total", count2);
							if (jsonObject2.containsKey(object.getString("bill_month") + "月")) {
								int cou = jsonObject2.getInteger(object.getString("bill_month") + "月");
								cou++;
								jsonObject2.put(object.getString("bill_month") + "月", cou);
								
							} else {
								jsonObject2.put(object.getString("bill_month") + "月", 1);

							}
							
							if (object.getString("location_type").contains("主叫")) {
								if (null == jsonObject2.getInteger(object.getString("bill_month") + "主叫")) {
									jsonObject2.put(object.getString("bill_month") + "主叫", 1);
								} else {
									jsonObject2.put(object.getString("bill_month") + "主叫", jsonObject2.getInteger(object.getString("bill_month") + "主叫") + 1);
								}
								
								if (null != object.getInteger("duration")) {
									jsonObject2.put(object.getString("bill_month") + "主叫时长", object.getIntValue("duration") + jsonObject2.getIntValue(object.getString("bill_month") + "主叫时长"));
								}
							} else if (object.getString("location_type").contains("被叫")){
								if (null == jsonObject2.getInteger(object.getString("bill_month") + "被叫")) {
									jsonObject2.put(object.getString("bill_month") + "被叫", 1);
								} else {
									jsonObject2.put(object.getString("bill_month") + "被叫", jsonObject2.getInteger(object.getString("bill_month") + "被叫") + 1);
								}
								
								if (null != object.getInteger("duration")) {
									jsonObject2.put(object.getString("bill_month") + "被叫时长", object.getIntValue("duration") + jsonObject2.getIntValue(object.getString("bill_month") + "被叫时长"));
								}
							} else {
								if (null == jsonObject2.getInteger(object.getString("bill_month") + "未知")) {
									jsonObject2.put(object.getString("bill_month") + "未知", 1);
								} else {
									jsonObject2.put(object.getString("bill_month") + "未知", jsonObject2.getInteger(object.getString("bill_month") + "未知") + 1);
								}
								
								if (null != object.getInteger("duration")) {
									jsonObject2.put(object.getString("bill_month") + "未知时长", object.getIntValue("duration") + jsonObject2.getIntValue(object.getString("bill_month") + "未知时长"));
								}
							}

							objectItem2.put("phone", object.getString("phone"));
							objectItem2.put("contactPhone", object.getString("peer_number"));
							objectItem2.put("bill_month", object.getString("bill_month"));
							objectItem2.put("location_type", object.getString("location_type"));
							objectItem2.put("time", object.getString("time"));

							list2.add(objectItem2);
						}

					}
					callRecord.add(jsonObject);
					callRecord.add(jsonObject2);

				}

				list.add(callRecord);

			}
		}

		return new ResponseEntity(ResponseEntity.STATUS_OK, null, "运营商原始信息为空", list);
	}

	/**
	 * 获取riskData数据
	 * 
	 * @param request
	 * @return
	 */
	public ResponseEntity getRiskData(DecisionHandleRequest request) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		JSONObject riskData = new JSONObject();
		riskData.put("mobile", request.getUserName()); // 手机号(必填),
		riskData.put("idNum", request.getCardId()); // 身份证号(必填),
		riskData.put("gender", IdcardUtils.getCHGenderByIdCard(request.getCardId())); // 性别(必填),
		riskData.put("apply_time", sdf.format(request.getApplyTime())); // 申请时间,

		this.genPresent(request);

		JSONObject present = new JSONObject(); // 居住地址信息(必填)
		present.put("province", request.getPresent().getString("province"));
		present.put("city", request.getPresent().getString("city"));
		present.put("liveAddr", request.getPresent().getString("liveAddr")); // 详细地址
		riskData.put("present", present);

		JSONObject job = new JSONObject();
		riskData.put("job", job);

		// 分数信息
		JSONArray score = new JSONArray();
		riskData.put("score", score);

		String operatorNum = "";
		String clientNum = "";
		if (StringUtils.isNotEmpty(request.getRobotRequestDTO().getOperatorNum()) && StringUtils.isNotEmpty(request.getRobotRequestDTO().getClientNum())) {
			operatorNum = request.getRobotRequestDTO().getOperatorNum();
			clientNum = request.getRobotRequestDTO().getClientNum();
		} else {
			DataOrderMapping dataOrderMapping = dataOrderMappingService.getLastOneByUserIdAndNid(request.getUserId(),
					request.getNid());
			if (null == dataOrderMapping) {
				return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "没有查询到映射借款单与三方数据映射信息", null);
			}

			operatorNum = dataOrderMapping.getOperatorNum();
			clientNum = dataOrderMapping.getClientNum();
		}

		Map<String, Object> queryMap = new HashMap<String, Object>();
		queryMap.put("number", clientNum);
		queryMap.put("clientId", request.getUserId());
		List<JSONObject> emergency_contacts = null;
		if (null != request.getRobotRequestDTO().getMainContacts()) {
			emergency_contacts = request.getRobotRequestDTO().getMainContacts();
		} else {
			emergency_contacts = this.getEqMongoData(queryMap, MongoCollections.DB_USER_MAIN_CONTACT);
		}

		JSONArray contracts = new JSONArray();
		riskData.put("emergency_contacts", contracts); // 紧急联系人信息(至少两个)

		// 重新包装mongo中查出来的数据
		if (null != emergency_contacts && emergency_contacts.size() > 0) {
			for (JSONObject object : emergency_contacts) {
				JSONObject contract = new JSONObject();
				contract.put("relation", object.getString("releation"));
				contract.put("name", object.getString("contacts"));
				contract.put("phone", object.getString("contactsPhone"));

				contracts.add(contract);
			}
		} else {
			return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "紧急联系人为空", null);
		}

		queryMap.clear();
		queryMap.put("number", operatorNum);
		queryMap.put("phone", request.getUserName());
		List<JSONObject> rawInfo = this.getEqMongoData(queryMap, MongoCollections.DB_OPERATOR_ORIGINAL_DATA);
		if (null != rawInfo && rawInfo.size() > 0) {
			JSONObject object = (JSONObject) rawInfo.get(0);

			JSONArray packages = JSON.parseObject(object.getString("packages"), JSONArray.class);
			object.put("packages", packages);

			JSONArray families = JSON.parseObject(object.getString("families"), JSONArray.class);
			object.put("families", families);

			JSONArray recharges = JSON.parseObject(object.getString("recharges"), JSONArray.class);
			object.put("recharges", recharges);

			JSONArray bills = JSON.parseObject(object.getString("bills"), JSONArray.class);
			object.put("bills", bills);

			JSONArray calls = JSON.parseObject(object.getString("calls"), JSONArray.class);
			object.put("calls", calls);

			JSONArray smses = JSON.parseObject(object.getString("smses"), JSONArray.class);
			object.put("smses", smses);

			JSONArray nets = JSON.parseObject(object.getString("nets"), JSONArray.class);
			object.put("nets", nets);

			JSONObject month_info = JSON.parseObject(object.getString("month_info"), JSONObject.class);
			object.put("month_info", month_info);

			riskData.put("rawInfo", object); // 运营商原始信息
//			riskData.put("rawInfo", rawInfo.get(0)); // 运营商原始信息
		} else {
			return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "运营商原始信息为空", null);
		}

		queryMap.clear();
		queryMap.put("number", operatorNum);
		queryMap.put("clientId", request.getUserId());

		List<JSONObject> rawReport = this.getEqMongoData(queryMap, MongoCollections.DB_OPERATOR_MOJIE_INFO);
		if (null != rawReport && rawReport.size() > 0) {
			JSONObject object = (JSONObject) rawReport.get(0);
			JSONObject report = JSON.parseObject(object.getString("clientReport"), JSONObject.class);
			riskData.put("rawReport", report); // 运营商报告信息

		} else {
			return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "运营商报告信息为空", null);
		}

		queryMap.clear();
		queryMap.put("number", clientNum);
		queryMap.put("clientId", request.getUserId());

		List<JSONObject> contacts = this.getEqMongoData(queryMap, MongoCollections.DB_DEVICE_CONTACT);
		JSONArray contact = new JSONArray();

		if (null != contacts && contacts.size() > 0) {
			for (JSONObject object : contacts) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("contact_name", object.getString("contacts"));
				jsonObject.put("contact_phone", object.getString("contactsPhone"));
				jsonObject.put("update_time", sdf.format(object.getDate("updateTime")));

				contact.add(jsonObject);
			}
		} else {
			return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "设备通讯录为空", null);
		}

		riskData.put("contact", contact); // 设备通讯录

		List<JSONObject> callRecords = this.getEqMongoData(queryMap, MongoCollections.DB_DEVICE_CALL_RECORD);

		JSONArray callRecord = new JSONArray();
		if (null != callRecords && callRecords.size() > 0) {
			for (JSONObject object : callRecords) {
				if (null != transferInOutType(object.getString("type"))) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("phone_number", object.getString("contactsPhone"));
					jsonObject.put("in_out", transferInOutType(object.getString("type"))); // //类型，1呼入，2呼出
					jsonObject.put("duration_time", object.getString("duration"));
					Date date = null;
					try {
						date = sdf.parse(object.getString("data"));
					} catch (ParseException e) {
						log.error("[转化设备通话记录呼叫时间出错]:{}", JSON.toJSONString(object));
						e.printStackTrace();
					}
					jsonObject.put("call_time", date.getTime()); // "call_time": "1509599164260", //呼叫时间
					jsonObject.put("add_time", String.valueOf(object.getDate("createTime").getTime()).substring(0, 10)); // "add_time":
																															// "1509599530"
																															// //数据获取时间

					callRecord.add(jsonObject);
				}
			}
		}

		riskData.put("callRecords", callRecord); // 设备通话记录

		List<JSONObject> userSms = this.getEqMongoData(queryMap, MongoCollections.DB_DEVICE_SMS);

		JSONArray userSmss = new JSONArray();
		if (null != userSms && userSms.size() > 0) {
			for (JSONObject object : userSms) {
				if (null != transferSmsType(object.getString("type"))) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("user_mobile", object.getString("phone"));
					jsonObject.put("peer_number", object.getString("contactsPhone"));
					jsonObject.put("content", object.getString("body"));
					jsonObject.put("sms_time", object.getString("data"));
					jsonObject.put("sms_type", transferSmsType(object.getString("type"))); // "sms_type": 1,
																							// //1接收；2发送；3:草稿箱；4：发件箱5发送失败；6:在排队发送
					jsonObject.put("update_time", sdf.format(object.getDate("updateTime"))); // "call_time":
																								// "1509599164260",
																								// //呼叫时间

					userSmss.add(jsonObject);
				}
			}
		}
		riskData.put("userSms", userSmss);
		riskData.put("app", new JSONArray());

		return new ResponseEntity(ResponseEntity.STATUS_OK, riskData);
	}

	public Integer transferSmsType(String type) {

		if (StringUtils.isNotEmpty(type)) {

			if (type.contains("排队发送")) {
				return 6;
			} else if (type.contains("发送失败")) {
				return 5;
			} else if (type.contains("发件箱")) {
				return 4;
			} else if (type.contains("草稿箱")) {
				return 3;
			} else if (type.contains("发送") || type.contains("发出")) {
				return 2;
			} else if (type.contains("接收") || type.contains("收到")) {
				return 1;
			} else {
				return null;
			}
		}

		return null;
	}

	public Integer transferInOutType(String type) {

		if (StringUtils.isNotEmpty(type)) {
			if (type.contains("呼入")) {
				return 1;
			} else if (type.contains("呼出")) {
				return 2;
			} else {
				return null;
			}
		}

		return null;
	}

	public void genPresent(DecisionHandleRequest request) {
		JSONObject result = new JSONObject();

		if (StringUtils.isNotEmpty(request.getLongitude()) && StringUtils.isNotEmpty(request.getLatitude())
				&& Double.parseDouble(request.getLongitude()) != 0 && Double.parseDouble(request.getLatitude()) != 0) {
			JSONObject resultObject = GetLocation.getAdd(request.getLongitude(), request.getLatitude());

			if (null != resultObject && null != resultObject.getJSONArray("addrList")) {
				JSONArray jsonArray = resultObject.getJSONArray("addrList");

				JSONObject j_2 = (JSONObject) jsonArray.get(0);
				String allAdd = j_2.getString("admName");

				if (StringUtils.isNotEmpty(allAdd)) {
					String[] arr = allAdd.split(",");

					if (arr.length > 0) {
						result.put("province", arr[0]);
						if (arr.length > 1) {
							result.put("city", arr[1]);
						} else {
							result.put("city", "");
						}
					} else {
						result.put("province", "");
						result.put("city", "");
					}

					StringBuilder sb = new StringBuilder();
					for (String tem : arr) {
						sb.append(tem);
					}
					result.put("liveAddr", sb.toString());

					if (StringUtils.isNotEmpty(j_2.getString("name"))) {
						result.put("liveAddr", sb.toString() + j_2.getString("name"));
					}

					request.setPresent(result);
					log.info("[{}经纬度获取到的地址]：{}", request.getNid(), JSON.toJSONString(result));

					return;

				}
			}
		}
		// 经纬不行拿身份证
		String cardId = request.getCardId();
		String provinceCode = cardId.substring(0, 2) + "0000";
		String cityCode = cardId.substring(0, 4) + "00";
		String distinctCode = cardId.substring(0, 6);

		String province = "";
		String city = "";
		String distinct = "";

		RiskArea area = new RiskArea();
		area.setProvinceCode(Integer.parseInt(provinceCode));
		area.setCityCode(Integer.parseInt(cityCode));
		area.setDistinctCode(Integer.parseInt(distinctCode));

		List<RiskArea> riskAreas = riskAreaDao.getByCodes(area);

		if (null != riskAreas) {
			for (RiskArea riskArea : riskAreas) {
				if (riskArea.getLevel() == 1) {
					result.put("province", riskArea.getName());
					province = riskArea.getName();
				} else if (riskArea.getLevel() == 2) {
					result.put("city", riskArea.getName());
					city = riskArea.getName();
				} else if (riskArea.getLevel() == 3) {
					distinct = riskArea.getName();
				}

				result.put("liveAddr", province + city + distinct);

			}
		}

		log.info("[{}身份证号获取到的地址]：{}", request.getNid(), JSON.toJSONString(result));

		request.setPresent(result);
		return;
	}

	/**
	 * 获取mongo中数据
	 * 
	 * @param collection
	 * @return
	 */
	@Override
	public List<JSONObject> getEqMongoData(Map<String, Object> queryMap, String collection) {
		List<MongoQuery> queries = new ArrayList<>();

		for (Map.Entry<String, Object> entry : queryMap.entrySet()) {
			MongoQuery query = new MongoQuery(entry.getKey(), entry.getValue(), MongoQuery.MongoQueryBaseType.eq);
			queries.add(query);
		}

		List<JSONObject> data = mongoDao.find(queries, JSONObject.class, collection, null);

		return data;
	}

	public ResponseEntity sendPaixu(DecisionHandleRequest req, String nids) {
		if (StringUtils.isNotEmpty(nids)) {
			String[] nidsArr = nids.split(",");

			if (nidsArr.length > 10) {
				return new ResponseEntity(ResponseEntity.STATUS_FAIL, null, "最多只能发10单！", null);

			}

			for (String nid : nidsArr) {
				try {
					RiskDecisionReqLog reqLog = riskDecisionReqLogDao.getLastBynid(nid.trim());
					ResponseEntity result = null;
					if (null != reqLog && StringUtils.isNotEmpty(reqLog.getReqData())) {
						DecisionHandleRequest decisionHandleRequest = JSON.parseObject(reqLog.getReqData(),
								DecisionHandleRequest.class);

						result = requestPaixu(decisionHandleRequest);
					}
					log.info("[{}]发送结果:{}", nid, JSON.toJSONString(result));
				} catch (Exception e) {
					log.error("发送排序失败nid:[{}]", nid);
					e.printStackTrace();
				}

			}

			return new ResponseEntity(ResponseEntity.STATUS_OK, null, "发送完成", null);

		}

		DecisionHandleRequest request = new DecisionHandleRequest();
		request.setCardId(req.getCardId());
		request.setUserName(req.getUserName());
		request.setNid(req.getNid());
		request.setName(req.getName());
		request.setUserId(req.getUserId());
		request.setApplyTime(System.currentTimeMillis());
		request.setLatitude(req.getLatitude());
		request.setLongitude(req.getLongitude());

		return new ResponseEntity(ResponseEntity.STATUS_OK, null, "发送完成", requestPaixu(request));
	}
	
	public String getLastMonth(Date date) {
		Calendar calendar = Calendar.getInstance();//日历对象
        calendar.setTime(date);//设置当前日期
        calendar.add(Calendar.MONTH, -1);//

        return sdf.format(calendar.getTime());
	
	}
}

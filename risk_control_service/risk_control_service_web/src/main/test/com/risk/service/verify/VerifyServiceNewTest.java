package com.risk.service.verify;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.SpringBootStart;
import com.risk.controller.service.dao.DecisionReqLogDao;
import com.risk.controller.service.dto.AdmissionResultDTO;
import com.risk.controller.service.dto.AdmissionRuleDTO;
import com.risk.controller.service.entity.DecisionReqLog;
import com.risk.controller.service.handler.VerifyHandler;
import com.risk.controller.service.handler.XinyanHandler;
import com.risk.controller.service.request.DecisionHandleRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhangtong on 2018/4/11 0011.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringBootStart.class)
public class VerifyServiceNewTest {
    @Resource
    private VerifyHandler verifyHandler;
    @Resource
    private XinyanHandler xinyanHandler;
    @Resource
    private DecisionReqLogDao reqLogDao;

    @Test
    public void verifyDevice() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserId(999L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        HashMap<String, String> map = new HashMap<>();
        map.put("maxCount", "2");
        rule.setSetting(map);
        AdmissionResultDTO record = verifyHandler.verifyDevice(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void verifyPhoneBlackList() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserName("13855136236");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        HashMap<String, String> map = new HashMap<>();
        AdmissionResultDTO record = verifyHandler.verifyPhoneBlackList(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }


    @Test
    public void verifyIdCardBlackList() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setCardId("2");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        HashMap<String, String> map = new HashMap<>();
        AdmissionResultDTO record = verifyHandler.verifyIdCardBlackList(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void verifyMinAge() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setCardId("500240200010220111");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        HashMap<String, String> map = new HashMap<>();
        map.put("minAge", "18");
        rule.setSetting(map);
        AdmissionResultDTO record = verifyHandler.verifyMinAge(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void verifyMaxAge() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setCardId("500240200010220111");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        HashMap<String, String> map = new HashMap<>();
        map.put("maxAge", "15");
        rule.setSetting(map);
        AdmissionResultDTO record = verifyHandler.verifyMaxAge(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void verifydevCallRecord() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218090716122631329");
        request.setUserId(125L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        HashMap<String, String> map = new HashMap<>();
        map.put("MinCount", "1");
        rule.setSetting(map);
        AdmissionResultDTO record = verifyHandler.verifydevCallRecord(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void verifySMS() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("118083113503887209");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        HashMap<String, String> map = new HashMap<>();
        map.put("MinCount", "1");
        rule.setSetting(map);
        AdmissionResultDTO record = verifyHandler.verifySMS(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void verifyContact() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218090716122631329");
        request.setUserId(124L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        HashMap<String, String> map = new HashMap<>();
        map.put("MinCount", "1");
        rule.setSetting(map);
        AdmissionResultDTO record = verifyHandler.verifyContact(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void verifyDeviceCount() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserId(37L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        HashMap<String, String> map = new HashMap<>();
        map.put("maxDevices", "2");
        rule.setSetting(map);
        AdmissionResultDTO record = verifyHandler.verifyDeviceCount(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void verifyContactKeyWord() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserId(37L);
        request.setDevicePlatform("android");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        rule.setSetting(set);
        request.setNid("118083113503887209");

        //
        set.put("SensitiveWord", "高炮,借条,黑户,白户,高利贷,空放,苏日嘎,阿里备案");
        set.put("SensitiveWordCount", "1");

        AdmissionResultDTO record = verifyHandler.verifyContactKeyWord(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");

    }


    @Test
    public void testverifySMSKeyWord() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("118083113503887209");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("KeyWord", "法催部,严重逾期,恶意透支,逃避欠款,逃避还款,催收录音,大耳窿,涉嫌,婊子,起诉");
        set.put("KeyWordCount", "1");
        rule.setSetting(set);

        String str = "";

        JSONArray array = JSONArray.parseArray(str);
        List<JSONObject> list = new ArrayList<>();
        for (Object o : array) {
            JSONObject json = (JSONObject) o;
            list.add(json);
        }
        request.getRobotRequestDTO().setSms(list);
        AdmissionResultDTO sms = verifyHandler.verifySMSKeyWord(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(sms));
        System.out.println("==============================================");

    }

    @Test
    public void testverifySMSSensitiveWord() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218103110214679753");
        request.setApplyTime(1540374375491L);
        request.getRobotRequestDTO().setClientNum("b1810301622251000000173559435");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("SensitiveWord", "欠款,欠债,拖欠,已逾期,已经逾期,吸毒,贩毒,抽大烟,麻古,麻果,k粉,冰妹,过不下去,活不下去,赌徒,赌输,输完,输光,飞叶子,溜冰,挂失,有点想念,咪咕视频");
        set.put("SensitiveWordCount", "1");
        set.put("NotSensitiveWord", "秒白条,交警,电费,水费,水电费,物业费,如已还款,若已还款,要不要");
        set.put("days","60");
        rule.setSetting(set);
        AdmissionResultDTO sms = verifyHandler.verifySMSSensitiveWord(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(sms));
        System.out.println("==============================================");
    }

    @Test
    public void testCheckYhbRiskSMS() {
        DecisionHandleRequest request = new DecisionHandleRequest();

        request.setNid("118083113503887209");
        request.setUserName("18880000023");
        request.setCardId("510230199910110111");
        request.setDevicePlatform("android");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("failFast", "1");
        set.put("blacklistSource", "yhb_risk_sms");
        set.put("blacklistLevel", "3");
        rule.setSetting(set);

        request.setName("张三");
        AdmissionResultDTO dto = verifyHandler.checkRiskSMS(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(dto));
        System.out.println("==============================================");
    }

    @Test
    public void testverifyContactIsRegister() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("118083113503887209");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("warningCount", "1");
        set.put("dangerCount", "1");
        rule.setSetting(set);
        AdmissionResultDTO dto = verifyHandler.verifyContactIsRegister(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(dto));
        System.out.println("==============================================");
    }

    @Test
    public void verifyContactNum() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.getRobotRequestDTO().setNumber("b1809071612261000000124339344");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("minCount", "40");
        rule.setSetting(set);

        AdmissionResultDTO record = verifyHandler.verifyContactNum(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");

    }

    @Test
    public void verifyShortNoAll() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("118083113503887209");
        request.getRobotRequestDTO().setNumber("b1809271108091000000016032045");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("minCount", "2");
        set.put("shortNos", "114,110,112,117,120,119,122,121,103,108,184,15921072222");
        set.put("count110", "2");
        rule.setSetting(set);
        AdmissionResultDTO record = verifyHandler.verifyShortNoAll(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }


    @Test
    public void verifyjxlCallRecord() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.getRobotRequestDTO().setNumber("b1809071612261000000124339344");
        request.setUserId(124L);
        request.setNid("218090716122631329");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("MinCount", "1");
        rule.setSetting(set);
        AdmissionResultDTO record = verifyHandler.verifyjxlCallRecord(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void verifyJuXinLiIsRealName() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.getRobotRequestDTO().setNumber("b1809071612261000000124339344");
        request.setNid("218090716122631329");
        request.setUserId(125L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("MinCount", "1");
        rule.setSetting(set);
        AdmissionResultDTO record = verifyHandler.verifyJuXinLiIsRealName(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record));
        System.out.println("==============================================");
    }

    @Test
    public void testVerifyMobildIsNew() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218101509165368532");
        request.setUserId(37L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("minCount", "24");
        rule.setSetting(set);
        AdmissionResultDTO dto = verifyHandler.verifyMobildIsNew(request, rule);
        System.out.println(JSONObject.toJSONString(dto));
    }

    @Test
    public void testVerifyAreaIdCardBlackList() {
        System.out.println("进入test");
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setCardId("659900401999999999");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("minCount", "11");
        rule.setSetting(set);
        AdmissionResultDTO dto = verifyHandler.verifyAreaIdCardBlackList(request, rule);
        System.out.println(JSONObject.toJSONString(dto));
    }

    @Test
    public void testVerifyEmergencyContact() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("118083113503887209");
        request.setUserName("13956961396");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        AdmissionResultDTO dto = verifyHandler.verifyEmergencyContact(request, rule);
        System.out.println(JSONObject.toJSONString(dto));
    }

    @Test
    public void testVerifyContactBlackList() {
        System.out.println("进入test");
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218090716122631329");
        request.setUserId(125L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        AdmissionResultDTO dto = verifyHandler.verifyContactBlackList(request, rule);
        System.out.println(JSONObject.toJSONString(dto));
    }

    @Test
    public void testverifyContactParentCall() {
        System.out.println("进入test");
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218090716122631329");


        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("callCount", "25");
        set.put("callTime", "2000");
        rule.setSetting(set);
        AdmissionResultDTO dto = verifyHandler.verifyMainContactCallCount(request, rule);
        System.out.println(JSONObject.toJSONString(dto));

        AdmissionResultDTO dto2 = verifyHandler.verifyMainContactCallTime(request, rule);
        System.out.println(JSONObject.toJSONString(dto2));

    }

    @Test
    public void verifyIdfaBlackList() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserId(37L);

        AdmissionRuleDTO rule = new AdmissionRuleDTO();

        AdmissionResultDTO record2 = verifyHandler.verifyImeiBlackList(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verifyPhoneContinuous() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserName("18883313255");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("length", "3");
        rule.setSetting(set);

        AdmissionResultDTO record2 = verifyHandler.verifyPhoneContinuous(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void ifyUserCharge() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("31807170712424052");
        request.getRobotRequestDTO().setNumber("b1809081205171000000124652091");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("avgCharge", "4500");
        set.put("maxCharge", "40000");
        set.put("month", "6");
        rule.setSetting(set);

        AdmissionResultDTO record2 = verifyHandler.verifyUserCharge(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verifyUserNation() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setUserNation("土家族");

        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("keys", "回,汉");
        rule.setSetting(set);

        AdmissionResultDTO record2 = verifyHandler.verifyUserNation(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verifyOpertorCount() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.getRobotRequestDTO().setNumber("b1809071612261000000124339344");
        request.setDevicePlatform("ios");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("android", "10");
        set.put("ios", "14");
        rule.setSetting(set);

        AdmissionResultDTO record2 = verifyHandler.verifyOpertorCount(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verifyPhone() {
        DecisionHandleRequest request = new DecisionHandleRequest();
        request.getRobotRequestDTO().setNumber("b1809071612261000000124339344");
        request.setUserName("13061759718");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        AdmissionResultDTO record2 = verifyHandler.verifyPhone(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verifyUserContactName() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.getRobotRequestDTO().setNumber("b1809071612261000000124339344");
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("nameKey", "[0-9]{0,6}");
        set.put("maxCount", "1");
        rule.setSetting(set);

        AdmissionResultDTO record2 = verifyHandler.verifyUserContactName(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verifyOperatorCallNumAndPeopleNum() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setNid("218091810415578876");
        request.setUserId(3303L);
        request.setLabelGroupId(1000L);
        request.setApplyTime(1537200000000L);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("days", "30");
        set.put("callNum", "2");
        set.put("peopleNum", "5");
        rule.setSetting(set);

        AdmissionResultDTO record2 = verifyHandler.verifyOperatorCallNumAndPeopleNum(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verify30DaysCallDetail() {

        DecisionReqLog reqLog = reqLogDao.getbyNid("218103007243914642");
        DecisionHandleRequest request = JSONObject.parseObject(reqLog.getReqData(), DecisionHandleRequest.class);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("callDetailDays", "7");
        set.put("callNumDays", "30");
        set.put("callDetailNum", "1");
        set.put("cntCallNum30", "10");
        set.put("allCallNum30", "100");
        rule.setSetting(set);


        AdmissionResultDTO record2 = verifyHandler.verify30DaysCallDetail(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

    @Test
    public void verifyMaxOverdueDay() {

        DecisionHandleRequest request = new DecisionHandleRequest();
        request.setMaxOverdueDay(5);
        AdmissionRuleDTO rule = new AdmissionRuleDTO();
        Map<String, String> set = new HashMap<>();
        set.put("maxOverdueDay", "5");
        rule.setSetting(set);

        AdmissionResultDTO record2 = verifyHandler.verifyMaxOverdueDay(request, rule);
        System.out.println("==============================================");
        System.out.println(JSONObject.toJSONString(record2));
        System.out.println("==============================================");
    }

}

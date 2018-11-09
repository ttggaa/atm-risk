import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.common.httpclient.HttpClientUtils;
import com.risk.controller.service.common.utils.DateConvert;
import com.risk.controller.service.common.utils.PhoneUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import com.risk.controller.service.request.DecisionHandleRequest;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhangtong on 2018/7/11 0011.
 */
public class Test001 {

    @Test
    public void test1() {
        JSONObject json = new JSONObject();
        json.put("open_time", "2018-08-01");
        Date openTime = json.getDate("open_time");
        int monthCount = DateConvert.getMonthDiff(new Date(), openTime);
        System.out.println(monthCount);

        String str = "2018-01";
        try {
            Date d = DateConvert.formatStrDate(str, "YYYY-mm");
            monthCount = DateConvert.getMonthDiff(new Date(), d);
            System.out.println(monthCount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void checkPhoneContinuous() {
        String phone = "188833323333";
        int length = 1;
        char[] nums = phone.toCharArray();
        for (int i = nums.length - 1; i > 0; i--) {
            if (nums[i] == nums[i - 1]) {
                length++;
            } else {
                break;
            }
        }
        System.out.println(length);
    }


    @Test
    public void t3() {
        String str1 = "[  \"{  \"contacts\" : \"周宪文\",  \"contactsPhone\" : \"155-1441-5728\",  \"releation\" : \"父母\"}\",  \"{  \"contacts\" : \"张通\",  \"contactsPhone\" : \"18883315432\",  \"releation\" : \"同事\"}\"]";
        String str2 = "张超";
        char[] sc1 = str1.toCharArray();
        char[] sc2 = str2.toCharArray();
        int count = 0;
        for (int i = 0; i < sc1.length; i++) {
            if (sc1[i] == sc2[i]) {
                count++;
            }
        }
        System.out.println(count);
    }

    private static String API_URL_KONGHAO = "https://api.253.com/open/unn/batch-ucheck";

    @Test
    public void konghao() throws Throwable {
        String str1 = "15366706786,18550041056,13554519077,13397275646,13954453665,15265922341";
        str1 = "18296086054,18791640931,13680906460,15814438056,13629614141,14769651498,13731037162,15100342120,13786611312,15913210850,15816728274,15089139185";
        str1 = "15842282212,13591219138,18648831600,13712844143,15563867248,15689935021,13967078759,18611167825";
        str1 = "13367855325,13629614141,13737518808,14769651498,18176692471,18187639682,13768760575,13044243222,18387607902";
        str1 = "15027953062,13091102523,15128888135,17303200105,18634146561,13731037162,15203208276,13084921642,15100342120,13832047081,15133005671,13333208971,15833035505,13279460034,17320663023,13064581417,17734122618,15176030507,15512872351,18303389427,15930903798,15830081610,18833066311,15932207091,18132104321,18730088667";
        str1 = "13542866870,15621775518,17319205027,14754967905,15903851275,15815123835,13149324162,15992222766,18521717369,17538555842,057157110061,051281889226,13539685987,15535568776,15089139185,02965631310,13124613854,19936465730,08138335849,051267505385,18156053286,17129262925,15986871959,18988800913,13546854406,15915571777,13727690318,15915570725,15586783663,18590330877,05366852772,17852710389,13013953153,13193334361,15816728274";
        Map<String, String> params = new HashMap<String, String>();
        params.put("appId", "iMyFCwQj");
        params.put("appKey", "2E3RiBQy");
        params.put("mobiles", str1);
        String result = HttpClientUtils.doPost(API_URL_KONGHAO, params);
        System.out.println(result);
    }

    private static String API_URL_YANGMAODANG = "https://api.253.com/open/wool/wcheck";

    @Test
    public void yangmaodang() throws Throwable {
        String[] str1 = "15366706786,18550041056,13554519077,13397275646,13954453665,15265922341".split(",");
        str1 = "18296086054,18791640931,13680906460,15814438056,13629614141,14769651498,13731037162,15100342120,13786611312,15913210850,15816728274,15089139185".split(",");
        str1 = "15842282212,13591219138,18648831600,13712844143,15563867248,15689935021,13967078759,18611167825".split(",");
        str1 = "13367855325,13629614141,13737518808,14769651498,18176692471,18187639682,13768760575,13044243222,18387607902".split(",");
        str1 = "15027953062,13091102523,15128888135,17303200105,18634146561,13731037162,15203208276,13084921642,15100342120,13832047081,15133005671,13333208971,15833035505,13279460034,17320663023,13064581417,17734122618,15176030507,15512872351,18303389427,15930903798,15830081610,18833066311,15932207091,18132104321,18730088667".split(",");
        str1 = "13542866870,15621775518,17319205027,14754967905,15903851275,15815123835,13149324162,15992222766,18521717369,17538555842,057157110061,051281889226,13539685987,15535568776,15089139185,02965631310,13124613854,19936465730,08138335849,051267505385,18156053286,17129262925,15986871959,18988800913,13546854406,15915571777,13727690318,15915570725,15586783663,18590330877,05366852772,17852710389,13013953153,13193334361,15816728274".split(",");

        for (String phone : str1) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("appId", "iMyFCwQj");
            params.put("appKey", "2E3RiBQy");
            params.put("mobile", phone);
            params.put("ip", "");
            String result = HttpClientUtils.doPost(API_URL_YANGMAODANG, params);
            System.out.println("phone:" + result);
        }
    }

    @Test
    public void konghaoAndyangmaodang() throws Throwable {
        String phone = "18817776572";

        Map<String, String> params = new HashMap<String, String>();
        params.put("appId", "iMyFCwQj");
        params.put("appKey", "2E3RiBQy");
        params.put("mobiles", phone);
        String result = HttpClientUtils.doPost(API_URL_KONGHAO, params);
        System.out.println(result);


        Map<String, String> params2 = new HashMap<String, String>();
        params2.put("appId", "iMyFCwQj");
        params2.put("appKey", "2E3RiBQy");
        params2.put("mobile", phone);
        params2.put("ip", "");
        String result2 = HttpClientUtils.doPost(API_URL_YANGMAODANG, params2);
        System.out.println(result2);
    }


    @Test
    public void t5() {
        String key = "欠款,欠债,拖欠,已逾期,已经逾期,吸毒,贩毒,抽大烟,麻古,麻果,k粉,冰妹,过不下去,活不下去,赌徒,赌输,输完,输光,飞叶子,溜冰,上门催收,滞纳金,逾期未还,还钱,扣款失败";
        key = "法催部,严重逾期,恶意透支,逃避欠款,逃避还款,催收录音,大耳窿,涉嫌违法,婊子,起诉,委外";
        String[] keys = key.split(",");
        File file = new File("C:\\Users\\Administrator\\Desktop\\1.log");
        BufferedReader reader = null;
        Set<String> set = new HashSet<>();
        List<String> listMsg = new ArrayList<>();
        int count = 0;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
//                tempString = new String(tempString.getBytes("GBK"),"UTF-8");
                for (String s : keys) {
                    if (tempString.indexOf(s) >= 0) {
                        set.add(s);
                        listMsg.add(tempString);
                        count++;
                        break;
                    }
                }
                line++;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(count);
        System.out.println(JSONObject.toJSONString(set));
        for (String s : listMsg) {
            System.out.println(s.trim());
        }
    }


    @Test
    public void getPhone() {

        File file = new File("C:\\Users\\Administrator\\Desktop\\7.txt");
        BufferedReader reader = null;
        Set<String> set = new HashSet<>();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        try {
            String tempString = null;
            reader = new BufferedReader(new FileReader(file));
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                sb.append(tempString);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = JSONArray.parseArray(sb.toString());
        for (Object o : jsonArray) {
            JSONObject json = (JSONObject) o;
            set.add(json.getString("clientId"));
        }
        System.out.println(set.size());
        System.out.println(StringUtils.join(set, ","));
    }

    @Test
    public void writeFile() {
        try {
            String path = "C:\\Users\\Administrator\\Desktop\\结果.txt";
            File file = new File(path);
            if (!file.exists())
                file.createNewFile();
            StringBuffer sb = new StringBuffer();
            sb.append("====================================\r\n");
            sb.append("你好\r\n");
            sb.append("你好\r\n");
            FileOutputStream out = new FileOutputStream(file, true); //如果追加方式用true

            out.write(sb.toString().getBytes("utf-8"));//注意需要转换对应的字符集
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void writeFile2(String fileName, String content) {
        try {
            String path = "C:\\Users\\Administrator\\Desktop\\订单号\\" + fileName + "结果.txt";
            File file = new File(path);
            if (!file.exists())
                file.createNewFile();
            FileOutputStream out = new FileOutputStream(file, true); //如果追加方式用true
            out.write(content.getBytes("utf-8"));//注意需要转换对应的字符集
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public String readFile(String path) {
        File file = new File("C:\\Users\\Administrator\\Desktop\\订单号\\" + path + ".txt");
        BufferedReader reader = null;
        Set<String> set = new HashSet<>();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        try {
            String tempString = null;
            reader = new BufferedReader(new FileReader(file));
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                sb.append(tempString);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        JSONObject result = JSONObject.parseObject(sb.toString());
        JSONObject data = result.getJSONObject("data");
        JSONArray jsonArray = data.getJSONArray("results");
        for (Object o : jsonArray) {
            JSONObject json = (JSONObject) o;
            if (json.getInteger("total") > 0) {
                set.add(json.getString("contactsPhone"));
            }
        }
        return StringUtils.join(set, ",");
    }


    @Test
    public void getAll() {
        String name = "218091116372653650";
        String phones = this.readFile(name);
        String konghao2 = this.konghao2(phones);
        String yangmaodang2 = this.yangmaodang2(phones);
        StringBuffer sb = new StringBuffer();
        sb.append(konghao2).append("=======================================\r\n").append(yangmaodang2);
        this.writeFile2(name, sb.toString());
    }


    public String yangmaodang2(String phones) {
        String[] str1 = phones.split(",");
        StringBuffer sb = new StringBuffer();
        for (String phone : str1) {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("appId", "iMyFCwQj");
                params.put("appKey", "2E3RiBQy");
                params.put("mobile", phone);
                params.put("ip", "");
                String result = HttpClientUtils.doPost(API_URL_YANGMAODANG, params);
                if (StringUtils.isNotBlank(result)) {
                    JSONObject jsonObject = JSONObject.parseObject(result);
                    JSONObject data = jsonObject.getJSONObject("data");
                    if (null != data && null != data.getString("status") && !data.getString("status").equals("W1")) {
                        sb.append("手机号码：").append(data.getString("mobile"));
                        sb.append(",状态").append(data.getString("status"));
                        sb.append(",描述：").append(data.getString("tag")).append("\r\n");
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public String konghao2(String str1) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("appId", "iMyFCwQj");
        params.put("appKey", "2E3RiBQy");
        params.put("mobiles", str1);
        try {
            StringBuffer sb = new StringBuffer();
            String result = HttpClientUtils.doPost(API_URL_KONGHAO, params);
            if (StringUtils.isNotBlank(result)) {
                JSONObject jsonObject = JSONObject.parseObject(result);
                JSONArray data = jsonObject.getJSONArray("data");
                if (null != data && data.size() > 0) {
                    for (Object datum : data) {
                        JSONObject back = (JSONObject) datum;
                        if (!"1".equals(back.getString("status"))) {
                            sb.append("手机号码：").append(back.getString("mobile"));
                            sb.append("状态：").append(back.getString("status")).append("\r\n");
                        }
                    }
                }
            }
            return sb.toString();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void checkName() {
        String str = "a123";
        Pattern pattern = Pattern.compile("[0-9]{0,6}");
        Matcher isNum = pattern.matcher(str);
        if (isNum.matches()) {
            System.out.println("短号");
        } else {
            System.out.println("费短号");
        }
    }

}

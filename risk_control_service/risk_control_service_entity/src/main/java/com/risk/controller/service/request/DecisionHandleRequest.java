package com.risk.controller.service.request;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.dto.RobotRequestDTO;
import com.risk.controller.service.entity.AdmissionResult;
import com.risk.controller.service.entity.AdmissionResultDetail;
import com.risk.controller.service.entity.RobotResult;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 风控用户信息校验的实体类
 *
 * @Author ZT
 * @create 2018-08-27
 */
@Data
public class DecisionHandleRequest {

    public static String DEVICE_IOS = "ios"; //设备类型
    public static String DEVICE_ANDROID = "android";//设备类型
    public static final Integer USER_GOOD = 2;//正常还款

    /**
     * 入参
     **/
    @NotEmpty(message = "身份证号不能为空")
    private String cardId;
    @NotEmpty(message = "用户手机号不能为空")
    private String userName;
    // 订单相关 start
    @NotEmpty(message = "订单ID不能为空")
    private String nid;
    @NotNull(message = "是否快速失败标记不能为空")
    private Integer failFast; // 快速失败标识, 1-快速失败
    private Integer isRobot = 1; // 1生产数据，0测试数据
    private String name; // 用户真实姓名
    private Long userId;
    private String devicePlatform; // 设备类型，ios,android
    private Long labelGroupId; //  用户标签
    @NotNull(message = "申请时间不能为空")
    private Long applyTime;
    private String userNation;// 用户名族

    private String longitude; // 经度
    private String latitude; // 纬度
    private String idAddr; // 身份证地址

    private Integer productId;// 产品id
    private BigDecimal amount;//借款金额
    private BigDecimal zmScore;//芝麻分
    private Integer maxOverdueDay;//最大逾期天数
    private Integer successRepayNum;//成功还款次数
    private String merchantCode;//商户代码
    private Integer source;//1生产数据，2训练数据

    private Integer deviceUsedNum;// 用户的设备被其他用户使用的人次
    private Integer userDeviceNum;// 用户使用设备个数
    private Integer cntRegisterNum;  //通讯录 注册个数
    private Integer optRegisterNum;  //运营商通话记录 注册个数
    private String userInfo;     // 用户设备信息:{"idfa":["766A3455-DFF9-4FC2-B2D5-A79FD90786A9","B1D5A066-AC70-4E1E-BF73-196FE48F8D6B","C6CCE840-9949-4663-B521-749F690BD809"],"imei":["863127038832439","866571039620540"],"mac":["020000000000","A80C6317708C"]}
    private Integer overdueNum;   //运营商通话记录手机在平台逾期个数

    /**
     * 入参结束
     **/

    private AdmissionResult admissionResult; // 决策结果
    private Map<Long, AdmissionResultDetail> resultDetailMap; // key: ruleId

    /**
     * 缓存数据
     **/
    private RobotRequestDTO robotRequestDTO = new RobotRequestDTO();

    // "present": {"province": 身份,  "city": 城市, "liveAddr": 详细地址  }
    private JSONObject present;// 居住地信息

    /**
     * 设置默认值
     */
    public void setDefaultValue() {
        // 默认产品id为1
        if (null == productId || productId <= 0) {
            productId = 1;
        }
        // 默认借款金额为1
        if (null == amount || BigDecimal.ZERO.compareTo(amount) >= 0) {
            amount = new BigDecimal(720);
        }
        // 默认商户号为征信商户号
        if (StringUtils.isBlank(merchantCode)) {
            merchantCode = "ZX00001";
        }
        // 设置是否保存数据
        if (null == source || source <= 0) {
            source = RobotResult.SOURCE_1;
        }
    }
}

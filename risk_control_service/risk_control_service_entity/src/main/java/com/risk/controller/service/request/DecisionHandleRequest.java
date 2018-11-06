package com.risk.controller.service.request;

import com.alibaba.fastjson.JSONObject;
import com.risk.controller.service.dto.RobotRequestDTO;
import com.risk.controller.service.entity.AdmissionResult;
import com.risk.controller.service.entity.AdmissionResultDetail;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
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

    public static final Long LABLEGROUPIDNEW_1 = 1000L;//新户
    public static final Long lableGroupIdOld = 1009L;//老户

    public static final Long lableGroupIdNew = 1005L; //新用户
    public static final Long lableGroupIdSNew = 1007L;// 次新户
    public static final Long lableGroupIdPass = 1001L;//老户
    public static final Long lableGroupIdReject = 1002L;//老户

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
    private Integer isRobot = 1; // 是否走模型, 9：不走模型 1：走模型
    private String name; // 用户真实姓名
    private Long userId;
    private String devicePlatform; // 设备类型，ios,android
    private Long labelGroupId; //  用户标签
    @NotNull(message = "申请时间不能为空")
    private Long applyTime;
    private String userNation;// 用户名族
    // "present": {"province": 身份,  "city": 城市, "liveAddr": 详细地址  }
    private JSONObject present;// 居住地信息

    private String longitude; // 经度
    private String latitude; // 纬度
    private String idAddr; // 身份证地址

    private Integer productId;// 产品id
    private BigDecimal amount;//借款金额
    private BigDecimal zmScore;//芝麻分
    private Integer maxOverdueDay;//最大逾期天数

    public Integer getProductId() {
        if (null == this.productId || this.productId <= 0) {
            return 1;
        } else {
            return this.productId;
        }
    }

    public BigDecimal getAmount() {
        if (null == this.amount || BigDecimal.ZERO.compareTo(this.amount) >= 0) {
            return new BigDecimal(720);
        } else {
            return this.amount;
        }
    }

    /**
     * 入参结束
     **/

    private AdmissionResult admissionResult; // 决策结果
    private Map<Long, AdmissionResultDetail> resultDetailMap; // key: ruleId

    /**
     * 缓存数据
     **/
    private RobotRequestDTO robotRequestDTO = new RobotRequestDTO();
}

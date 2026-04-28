package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值订单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("recharge_order")
public class RechargeOrder extends BaseDo {

    private String orderNo;

    private Long userId;

    private Long planId;

    private BigDecimal amountYuan;

    /** PENDING / PAID / CANCELLED / REFUNDED */
    private String status;

    /** WECHAT_MOCK / ALIPAY_MOCK */
    private String mockPayMethod;

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    private String refundRemark;
}

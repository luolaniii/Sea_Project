package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 钱包流水
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wallet_transaction")
public class WalletTransaction extends BaseDo {

    private Long userId;

    /** EARN_BADGE / EARN_REVIEW / RECHARGE_BONUS / SPEND / REFUND */
    private String type;

    /** 正数为增加，负数为扣减 */
    private Integer amount;

    /** 操作后余额 */
    private Integer balanceAfter;

    /** 关联类型：BADGE / EVALUATION / ORDER 等 */
    private String refType;

    private Long refId;

    private String remark;
}

package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户钱包
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_wallet")
public class UserWallet extends BaseDo {

    private Long userId;

    private Integer balanceCoins;

    private Integer totalEarnedCoins;

    private Integer totalSpentCoins;
}

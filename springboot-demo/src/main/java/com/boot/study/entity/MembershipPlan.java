package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 会员套餐
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_plan")
public class MembershipPlan extends BaseDo {

    private String planCode;

    private String name;

    private String description;

    private Integer days;

    private BigDecimal priceYuan;

    private Integer bonusCoins;

    private Integer sortOrder;

    private Integer status;
}

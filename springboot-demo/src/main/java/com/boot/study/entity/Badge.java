package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 徽章定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("badge")
public class Badge extends BaseDo {

    private String code;

    private String name;

    private String description;

    private String iconEmoji;

    /** 解锁阈值（评估次数） */
    private Integer thresholdCount;

    private Integer rewardCoins;

    private Integer sortOrder;

    /** 0-停用 1-启用 */
    private Integer status;
}

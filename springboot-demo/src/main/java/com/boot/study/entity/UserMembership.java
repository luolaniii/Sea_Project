package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户会员状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_membership")
public class UserMembership extends BaseDo {

    private Long userId;

    private Long planId;

    private LocalDateTime startedAt;

    private LocalDateTime expiresAt;

    private Long lastOrderId;
}

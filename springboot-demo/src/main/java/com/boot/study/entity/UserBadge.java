package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户已获得徽章
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_badge")
public class UserBadge extends BaseDo {

    private Long userId;

    private Long badgeId;

    private Integer evaluatedCountSnapshot;

    private LocalDateTime awardedAt;
}

package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 论坛帖子评估实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_post_evaluation")
public class ForumPostEvaluation extends BaseDo {

    private Long postId;

    private Long evaluatorId;

    private String evaluatorName;

    private Integer isExpert;

    private BigDecimal scoreAccuracy;

    private BigDecimal scoreRisk;

    private BigDecimal scoreReasoning;

    private BigDecimal totalScore;

    private String comment;

    /**
     * 状态：0-无效, 1-有效
     */
    private Integer status;
}

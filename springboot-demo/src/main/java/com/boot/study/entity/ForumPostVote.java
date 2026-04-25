package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 论坛帖子投票实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_post_vote")
public class ForumPostVote extends BaseDo {

    private Long postId;

    private Long userId;

    /**
     * 投票类型：1-支持, -1-反对
     */
    private Integer voteType;
}

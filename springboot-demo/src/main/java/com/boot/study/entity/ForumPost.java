package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 论坛帖子实体
 * <p>
 * 对应数据库表：forum_post
 * 用于存储论坛帖子的基本信息，包括标题、内容、作者、分类等
 *
 * @author study
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_post")
public class ForumPost extends BaseDo {

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 作者ID（关联sys_user表）
     */
    private Long authorId;

    /**
     * 作者用户名（冗余字段，便于查询）
     */
    private String authorName;

    /**
     * 分类：GENERAL-普通讨论, QUESTION-问题求助, SHARE-经验分享, NEWS-新闻资讯
     */
    private String category;

    /**
     * 帖子类型：TOPIC_DISCUSSION-主题讨论, DATA_ANALYSIS-数据分析
     */
    private String postType;

    /**
     * 分析目标（当postType=DATA_ANALYSIS时）
     */
    private String analysisTarget;

    /**
     * 可靠性状态：0-未评选, 1-评选中, 2-已认证
     */
    private Integer reliabilityStatus;

    /**
     * 可靠性综合评分（0-100）
     */
    private java.math.BigDecimal reliabilityScore;

    /**
     * 专家评估次数
     */
    private Integer evaluationCount;

    /**
     * 标签（JSON数组格式，如：["海洋", "波浪", "数据"]）
     */
    private String tags;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 是否置顶：0-否, 1-是
     */
    private Integer isTop;

    /**
     * 是否精华：0-否, 1-是
     */
    private Integer isEssence;

    /**
     * 状态：0-草稿, 1-已发布, 2-已关闭
     */
    private Integer status;

    /**
     * 是否允许评论：0-否, 1-是
     */
    private Integer allowComment;
}


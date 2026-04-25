package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 专家答疑实体
 * <p>
 * 对应数据库表：expert_answer
 * 用于存储专家对用户问题的专业回答
 *
 * @author study
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("expert_answer")
public class ExpertAnswer extends BaseDo {

    /**
     * 关联的帖子ID（关联forum_post表）
     */
    private Long postId;

    /**
     * 关联的评论ID（如果是对评论的回答，关联forum_comment表）
     */
    private Long commentId;

    /**
     * 专家ID（关联sys_user表，role为expert的用户）
     */
    private Long expertId;

    /**
     * 专家用户名（冗余字段）
     */
    private String expertName;

    /**
     * 专家职称/头衔
     */
    private String expertTitle;

    /**
     * 回答内容
     */
    private String answerContent;

    /**
     * 回答类型：QA-答疑, EVALUATION-评估意见
     */
    private String answerType;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 是否采纳：0-否, 1-是
     */
    private Integer isAccepted;

    /**
     * 状态：0-正常, 1-已删除
     */
    private Integer status;
}


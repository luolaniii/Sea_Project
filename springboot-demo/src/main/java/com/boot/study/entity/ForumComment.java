package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 论坛评论实体
 * <p>
 * 对应数据库表：forum_comment
 * 用于存储论坛帖子的评论信息，支持多级回复
 *
 * @author study
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_comment")
public class ForumComment extends BaseDo {

    /**
     * 帖子ID（关联forum_post表）
     */
    private Long postId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论人ID（关联sys_user表）
     */
    private Long userId;

    /**
     * 评论人用户名（冗余字段，便于查询）
     */
    private String userName;

    /**
     * 父评论ID（用于多级回复，0表示顶级评论）
     */
    private Long parentId;

    /**
     * 回复的用户ID（如果是对某个用户的回复）
     */
    private Long replyToUserId;

    /**
     * 回复的用户名（冗余字段）
     */
    private String replyToUserName;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 状态：0-正常, 1-已删除, 2-已屏蔽
     */
    private Integer status;

    /**
     * 子评论列表（不映射到数据库，仅用于返回树形结构）
     */
    @TableField(exist = false)
    private List<ForumComment> children;
}


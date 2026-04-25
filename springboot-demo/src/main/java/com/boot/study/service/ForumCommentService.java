package com.boot.study.service;

import com.boot.study.entity.ForumComment;

import java.util.List;

/**
 * 论坛评论 Service
 */
public interface ForumCommentService extends BaseService<ForumComment> {

    /**
     * 根据帖子ID查询评论列表（树形结构）
     *
     * @param postId 帖子ID
     * @return 评论列表
     */
    List<ForumComment> getCommentsByPostId(Long postId);

    /**
     * 添加评论
     *
     * @param comment 评论信息
     * @return 保存后的评论
     */
    ForumComment addComment(ForumComment comment);

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     * @return 是否成功
     */
    boolean likeComment(Long commentId);

    /**
     * 取消点赞评论
     *
     * @param commentId 评论ID
     * @return 是否成功
     */
    boolean unlikeComment(Long commentId);

    /**
     * 更新评论
     *
     * @param comment 评论信息
     * @return 更新后的评论
     */
    ForumComment updateComment(ForumComment comment);

    /**
     * 删除评论
     *
     * @param id 评论ID
     */
    void deleteComment(Long id);

    /**
     * 根据父评论ID查询子评论列表
     *
     * @param parentId 父评论ID
     * @return 子评论列表
     */
    List<ForumComment> getRepliesByParentId(Long parentId);

    /**
     * 根据帖子ID批量删除评论
     *
     * @param postId 帖子ID
     */
    void deleteCommentsByPostId(Long postId);
}


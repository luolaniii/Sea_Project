package com.boot.study.service;

import com.boot.study.entity.ExpertAnswer;

import java.util.List;

/**
 * 专家答疑 Service
 */
public interface ExpertAnswerService extends BaseService<ExpertAnswer> {

    /**
     * 根据帖子ID查询专家回答列表
     *
     * @param postId 帖子ID
     * @return 专家回答列表
     */
    List<ExpertAnswer> getAnswersByPostId(Long postId);

    /**
     * 根据评论ID查询专家回答列表
     *
     * @param commentId 评论ID
     * @return 专家回答列表
     */
    List<ExpertAnswer> getAnswersByCommentId(Long commentId);

    /**
     * 添加专家回答
     *
     * @param answer 回答信息
     * @return 保存后的回答
     */
    ExpertAnswer addAnswer(ExpertAnswer answer);

    /**
     * 采纳专家回答
     *
     * @param answerId 回答ID
     * @return 是否成功
     */
    boolean acceptAnswer(Long answerId);

    /**
     * 点赞专家回答
     *
     * @param answerId 回答ID
     * @return 是否成功
     */
    boolean likeAnswer(Long answerId);

    /**
     * 取消点赞专家回答
     *
     * @param answerId 回答ID
     * @return 是否成功
     */
    boolean unlikeAnswer(Long answerId);

    /**
     * 更新专家回答
     *
     * @param answer 回答信息
     * @return 更新后的回答
     */
    ExpertAnswer updateAnswer(ExpertAnswer answer);

    /**
     * 删除专家回答
     *
     * @param id 回答ID
     */
    void deleteAnswer(Long id);
}


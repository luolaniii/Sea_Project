package com.boot.study.service;

import com.boot.study.entity.ForumPostEvaluation;

import java.util.List;

/**
 * 论坛帖子评估 Service
 */
public interface ForumPostEvaluationService extends BaseService<ForumPostEvaluation> {

    ForumPostEvaluation submitEvaluation(ForumPostEvaluation evaluation);

    List<ForumPostEvaluation> listByPostId(Long postId);
}

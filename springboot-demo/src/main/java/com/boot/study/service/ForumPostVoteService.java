package com.boot.study.service;

import com.boot.study.entity.ForumPostVote;

import java.util.Map;

/**
 * 论坛帖子投票 Service
 */
public interface ForumPostVoteService extends BaseService<ForumPostVote> {

    ForumPostVote vote(Long postId, Integer voteType);

    Map<String, Object> getVoteSummary(Long postId);
}

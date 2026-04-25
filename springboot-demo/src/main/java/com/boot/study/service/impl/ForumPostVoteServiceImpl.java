package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boot.study.bean.LoginInfo;
import com.boot.study.dao.ForumPostVoteMapper;
import com.boot.study.entity.ForumPost;
import com.boot.study.entity.ForumPostVote;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.ForumPostService;
import com.boot.study.service.ForumPostVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 论坛帖子投票 Service 实现
 */
@Service
@RequiredArgsConstructor
public class ForumPostVoteServiceImpl extends BaseServiceImpl<ForumPostVoteMapper, ForumPostVote> implements ForumPostVoteService {

    private final ForumPostService forumPostService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPostVote vote(Long postId, Integer voteType) {
        if (LoginInfo.getLoginInfo() == null || LoginInfo.getUserId() == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        if (voteType == null || (voteType != 1 && voteType != -1)) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "voteType只能为1或-1");
        }
        ForumPost post = forumPostService.getById(postId);
        if (post == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "帖子不存在");
        }
        if (!"DATA_ANALYSIS".equals(post.getPostType())) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "仅数据分析类型帖子支持评选投票");
        }

        Long userId = LoginInfo.getUserId();
        ForumPostVote existing = getOne(new LambdaQueryWrapper<ForumPostVote>()
                .eq(ForumPostVote::getPostId, postId)
                .eq(ForumPostVote::getUserId, userId)
                .last("limit 1"));
        if (existing == null) {
            existing = new ForumPostVote();
            existing.setPostId(postId);
            existing.setUserId(userId);
            existing.setVoteType(voteType);
            save(existing);
            return existing;
        }
        existing.setVoteType(voteType);
        updateById(existing);
        return existing;
    }

    @Override
    public Map<String, Object> getVoteSummary(Long postId) {
        Long support = count(new LambdaQueryWrapper<ForumPostVote>()
                .eq(ForumPostVote::getPostId, postId)
                .eq(ForumPostVote::getVoteType, 1));
        Long oppose = count(new LambdaQueryWrapper<ForumPostVote>()
                .eq(ForumPostVote::getPostId, postId)
                .eq(ForumPostVote::getVoteType, -1));

        Map<String, Object> result = new HashMap<>();
        result.put("postId", postId);
        result.put("supportCount", support);
        result.put("opposeCount", oppose);
        result.put("totalCount", support + oppose);
        return result;
    }
}

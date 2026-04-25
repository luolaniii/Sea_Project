package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boot.study.bean.LoginInfo;
import com.boot.study.dao.ExpertAnswerMapper;
import com.boot.study.entity.ExpertAnswer;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.ExpertAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 专家答疑 Service 实现
 */
@Slf4j
@Service
public class ExpertAnswerServiceImpl extends BaseServiceImpl<ExpertAnswerMapper, ExpertAnswer> implements ExpertAnswerService {

    @Override
    public List<ExpertAnswer> getAnswersByPostId(Long postId) {
        LambdaQueryWrapper<ExpertAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpertAnswer::getPostId, postId)
                .eq(ExpertAnswer::getStatus, 0)
                .orderByDesc(ExpertAnswer::getIsAccepted) // 采纳的回答优先
                .orderByDesc(ExpertAnswer::getLikeCount)
                .orderByDesc(ExpertAnswer::getCreatedTime);
        return list(wrapper);
    }

    @Override
    public List<ExpertAnswer> getAnswersByCommentId(Long commentId) {
        LambdaQueryWrapper<ExpertAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpertAnswer::getCommentId, commentId)
                .eq(ExpertAnswer::getStatus, 0)
                .orderByDesc(ExpertAnswer::getIsAccepted)
                .orderByDesc(ExpertAnswer::getLikeCount)
                .orderByDesc(ExpertAnswer::getCreatedTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertAnswer addAnswer(ExpertAnswer answer) {
        if (LoginInfo.getLoginInfo() == null || LoginInfo.getUserId() == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        String role = LoginInfo.getUserRole();
        if (!"expert".equals(role) && !"admin".equals(role)) {
            throw new ServiceException(403, "仅专家或管理员可以发布专家答疑");
        }
        answer.setExpertId(LoginInfo.getUserId());
        if (LoginInfo.getLoginInfo().getUsername() != null) {
            answer.setExpertName(LoginInfo.getLoginInfo().getUsername());
        }

        // 设置默认值
        if (answer.getAnswerType() == null) {
            answer.setAnswerType("QA");
        }
        if (answer.getLikeCount() == null) {
            answer.setLikeCount(0);
        }
        if (answer.getIsAccepted() == null) {
            answer.setIsAccepted(0);
        }
        if (answer.getStatus() == null) {
            answer.setStatus(0); // 默认正常状态
        }
        
        save(answer);
        return answer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptAnswer(Long answerId) {
        ExpertAnswer answer = getById(answerId);
        if (answer == null) {
            throw new ServiceException(404, "回答不存在");
        }
        // 如果该帖子/评论已有采纳的回答，先取消其他回答的采纳状态
        if (answer.getPostId() != null) {
            List<ExpertAnswer> existingAccepted = list(new LambdaQueryWrapper<ExpertAnswer>()
                    .eq(ExpertAnswer::getPostId, answer.getPostId())
                    .eq(ExpertAnswer::getIsAccepted, 1)
                    .ne(ExpertAnswer::getId, answerId));
            existingAccepted.forEach(a -> {
                a.setIsAccepted(0);
                updateById(a);
            });
        }
        
        answer.setIsAccepted(1);
        boolean success = updateById(answer);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeAnswer(Long answerId) {
        ExpertAnswer answer = getById(answerId);
        if (answer == null) {
            throw new ServiceException(404, "回答不存在");
        }
        answer.setLikeCount((answer.getLikeCount() == null ? 0 : answer.getLikeCount()) + 1);
        boolean success = updateById(answer);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlikeAnswer(Long answerId) {
        ExpertAnswer answer = getById(answerId);
        if (answer == null) {
            throw new ServiceException(404, "回答不存在");
        }
        if (answer.getLikeCount() == null || answer.getLikeCount() <= 0) {
            throw new ServiceException(400, "点赞数已为0，无法取消");
        }
        answer.setLikeCount(answer.getLikeCount() - 1);
        boolean success = updateById(answer);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpertAnswer updateAnswer(ExpertAnswer answer) {
        if (answer.getId() == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "回答ID不能为空");
        }
        boolean success = updateById(answer);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return answer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnswer(Long id) {
        boolean success = removeById(id);
        if (!success) {
            throw new ServiceException(ResultEnum.DELETE_FAILED);
        }
    }
}


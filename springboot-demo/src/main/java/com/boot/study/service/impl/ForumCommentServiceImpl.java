package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boot.study.bean.LoginInfo;
import com.boot.study.dao.ForumCommentMapper;
import com.boot.study.dao.ForumPostMapper;
import com.boot.study.entity.ForumComment;
import com.boot.study.entity.ForumPost;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.ForumCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 论坛评论 Service 实现
 */
@Slf4j
@Service
public class ForumCommentServiceImpl extends BaseServiceImpl<ForumCommentMapper, ForumComment> implements ForumCommentService {

    @Autowired
    private ForumPostMapper forumPostMapper;

    @Override
    public List<ForumComment> getCommentsByPostId(Long postId) {
        LambdaQueryWrapper<ForumComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumComment::getPostId, postId)
                .eq(ForumComment::getStatus, 0) // 只查询正常状态的评论
                .orderByAsc(ForumComment::getCreatedTime);
        
        List<ForumComment> allComments = list(wrapper);
        
        // 构建树形结构：顶级评论（parentId为0或null）
        return allComments.stream()
                .filter(comment -> comment.getParentId() == null || comment.getParentId() == 0)
                .map(comment -> buildCommentTree(comment, allComments))
                .collect(Collectors.toList());
    }

    /**
     * 递归构建评论树
     */
    private ForumComment buildCommentTree(ForumComment parent, List<ForumComment> allComments) {
        List<ForumComment> children = allComments.stream()
                .filter(comment -> parent.getId().equals(comment.getParentId()))
                .map(comment -> buildCommentTree(comment, allComments))
                .collect(Collectors.toList());
        
        // 将子评论列表直接设置到children字段
        if (!children.isEmpty()) {
            parent.setChildren(children);
        }
        
        return parent;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumComment addComment(ForumComment comment) {
        // 设置当前登录用户信息
        if (LoginInfo.getLoginInfo() == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        Long userId = LoginInfo.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        comment.setUserId(userId);
        
        // 设置用户名（如果LoginInfo中有）
        if (LoginInfo.getLoginInfo().getUsername() != null) {
            comment.setUserName(LoginInfo.getLoginInfo().getUsername());
        }
        
        // 设置默认值
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }
        if (comment.getStatus() == null) {
            comment.setStatus(0); // 默认正常状态
        }
        if (comment.getParentId() == null) {
            comment.setParentId(0L); // 默认顶级评论
        }
        
        save(comment);
        
        // 更新帖子的评论数
        if (comment.getPostId() != null) {
            ForumPost post = forumPostMapper.selectById(comment.getPostId());
            if (post != null) {
                post.setCommentCount((post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1);
                forumPostMapper.updateById(post);
            }
        }
        
        return comment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeComment(Long commentId) {
        ForumComment comment = getById(commentId);
        if (comment == null) {
            throw new ServiceException(404, "评论不存在");
        }
        comment.setLikeCount((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1);
        boolean success = updateById(comment);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlikeComment(Long commentId) {
        ForumComment comment = getById(commentId);
        if (comment == null) {
            throw new ServiceException(404, "评论不存在");
        }
        if (comment.getLikeCount() == null || comment.getLikeCount() <= 0) {
            throw new ServiceException(400, "点赞数已为0，无法取消");
        }
        comment.setLikeCount(comment.getLikeCount() - 1);
        boolean success = updateById(comment);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumComment updateComment(ForumComment comment) {
        if (comment.getId() == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "评论ID不能为空");
        }
        boolean success = updateById(comment);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return comment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        ForumComment comment = getById(id);
        if (comment == null) {
            throw new ServiceException(404, "评论不存在");
        }
        
        // 检查权限：只能删除自己的评论
        Long currentUserId = LoginInfo.getUserId();
        if (currentUserId == null || !currentUserId.equals(comment.getUserId())) {
            throw new ServiceException(403, "无权删除此评论，只能删除自己的评论");
        }
        
        // 删除评论（逻辑删除）
        boolean success = removeById(id);
        if (!success) {
            throw new ServiceException(ResultEnum.DELETE_FAILED);
        }
        
        // 更新帖子的评论数
        if (comment.getPostId() != null) {
            ForumPost post = forumPostMapper.selectById(comment.getPostId());
            if (post != null && post.getCommentCount() != null && post.getCommentCount() > 0) {
                post.setCommentCount(post.getCommentCount() - 1);
                forumPostMapper.updateById(post);
            }
        }
    }

    @Override
    public List<ForumComment> getRepliesByParentId(Long parentId) {
        LambdaQueryWrapper<ForumComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumComment::getParentId, parentId)
                .eq(ForumComment::getStatus, 0)
                .orderByAsc(ForumComment::getCreatedTime);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommentsByPostId(Long postId) {
        LambdaQueryWrapper<ForumComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumComment::getPostId, postId);
        // 批量删除该帖子下的所有评论（逻辑删除）
        remove(wrapper);
    }
}


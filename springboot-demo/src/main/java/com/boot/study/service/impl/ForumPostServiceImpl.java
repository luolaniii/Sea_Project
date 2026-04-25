package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.bean.LoginInfo;
import com.boot.study.dao.ForumPostMapper;
import com.boot.study.entity.ForumPost;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.response.PageBean;
import com.boot.study.service.ForumCommentService;
import com.boot.study.service.ForumPostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 论坛帖子 Service 实现
 */
@Slf4j
@Service
public class ForumPostServiceImpl extends BaseServiceImpl<ForumPostMapper, ForumPost> implements ForumPostService {

    @Autowired
    private ForumCommentService forumCommentService;

    @Override
    public IPage<ForumPost> pagePosts(Page<ForumPost> page, String category, String keyword, String postType, Integer reliabilityStatus) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询已发布的帖子
        wrapper.eq(ForumPost::getStatus, 1);
        
        // 分类筛选
        if (StringUtils.hasText(category)) {
            wrapper.eq(ForumPost::getCategory, category);
        }
        if (StringUtils.hasText(postType)) {
            wrapper.eq(ForumPost::getPostType, postType);
        }
        if (reliabilityStatus != null) {
            wrapper.eq(ForumPost::getReliabilityStatus, reliabilityStatus);
        }
        
        // 关键词搜索（标题或内容）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ForumPost::getTitle, keyword)
                    .or()
                    .like(ForumPost::getContent, keyword));
        }
        
        // 排序：置顶优先，然后按创建时间倒序
        wrapper.orderByDesc(ForumPost::getIsTop)
                .orderByDesc(ForumPost::getCreatedTime);
        
        return page(page, wrapper);
    }

    @Override
    public PageBean<ForumPost> pagePostsWithPageBean(Page<ForumPost> page, String category, String keyword, String postType, Integer reliabilityStatus) {
        IPage<ForumPost> iPage = pagePosts(page, category, keyword, postType, reliabilityStatus);
        return PageBean.page(iPage, iPage.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPost getPostDetail(Long id) {
        ForumPost post = getById(id);
        if (post != null && post.getStatus() == 1) {
            // 增加浏览量
            post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
            updateById(post);
        }
        return post;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPost publishPost(ForumPost post) {
        // 设置当前登录用户信息
        if (LoginInfo.getLoginInfo() == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        Long userId = LoginInfo.getUserId();
        if (userId == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        post.setAuthorId(userId);
        
        // 设置作者用户名（如果LoginInfo中有）
        if (LoginInfo.getLoginInfo().getUsername() != null) {
            post.setAuthorName(LoginInfo.getLoginInfo().getUsername());
        }
        
        // 设置默认值
        if (post.getViewCount() == null) {
            post.setViewCount(0);
        }
        if (post.getLikeCount() == null) {
            post.setLikeCount(0);
        }
        if (post.getCommentCount() == null) {
            post.setCommentCount(0);
        }
        if (post.getIsTop() == null) {
            post.setIsTop(0);
        }
        if (post.getIsEssence() == null) {
            post.setIsEssence(0);
        }
        if (post.getStatus() == null) {
            post.setStatus(1); // 默认已发布
        }
        if (post.getAllowComment() == null) {
            post.setAllowComment(1); // 默认允许评论
        }
        if (!StringUtils.hasText(post.getPostType())) {
            post.setPostType("TOPIC_DISCUSSION");
        }
        if (post.getReliabilityStatus() == null) {
            post.setReliabilityStatus(0);
        }
        if (post.getReliabilityScore() == null) {
            post.setReliabilityScore(java.math.BigDecimal.ZERO);
        }
        if (post.getEvaluationCount() == null) {
            post.setEvaluationCount(0);
        }
        
        save(post);
        return post;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likePost(Long postId) {
        ForumPost post = getById(postId);
        if (post == null) {
            throw new ServiceException(404, "帖子不存在");
        }
        post.setLikeCount((post.getLikeCount() == null ? 0 : post.getLikeCount()) + 1);
        boolean success = updateById(post);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlikePost(Long postId) {
        ForumPost post = getById(postId);
        if (post == null) {
            throw new ServiceException(404, "帖子不存在");
        }
        if (post.getLikeCount() == null || post.getLikeCount() <= 0) {
            throw new ServiceException(400, "点赞数已为0，无法取消");
        }
        post.setLikeCount(post.getLikeCount() - 1);
        boolean success = updateById(post);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return true;
    }

    @Override
    public List<ForumPost> getHotPosts(Integer limit) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumPost::getStatus, 1)
                .orderByDesc(ForumPost::getViewCount)
                .orderByDesc(ForumPost::getLikeCount)
                .orderByDesc(ForumPost::getCreatedTime)
                .last("limit " + (limit == null ? 10 : limit));
        return list(wrapper);
    }

    @Override
    public List<ForumPost> getTopPosts() {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumPost::getStatus, 1)
                .eq(ForumPost::getIsTop, 1)
                .orderByDesc(ForumPost::getCreatedTime);
        return list(wrapper);
    }

    @Override
    public IPage<ForumPost> pageMyPosts(Page<ForumPost> page, Long userId, String category, String keyword, String postType, Integer reliabilityStatus) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询当前用户的帖子
        wrapper.eq(ForumPost::getAuthorId, userId);
        
        // 分类筛选
        if (StringUtils.hasText(category)) {
            wrapper.eq(ForumPost::getCategory, category);
        }
        if (StringUtils.hasText(postType)) {
            wrapper.eq(ForumPost::getPostType, postType);
        }
        if (reliabilityStatus != null) {
            wrapper.eq(ForumPost::getReliabilityStatus, reliabilityStatus);
        }
        
        // 关键词搜索（标题或内容）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ForumPost::getTitle, keyword)
                    .or()
                    .like(ForumPost::getContent, keyword));
        }
        
        // 排序：按创建时间倒序
        wrapper.orderByDesc(ForumPost::getCreatedTime);
        
        return page(page, wrapper);
    }

    @Override
    public PageBean<ForumPost> pageMyPostsWithPageBean(Page<ForumPost> page, Long userId, String category, String keyword, String postType, Integer reliabilityStatus) {
        IPage<ForumPost> iPage = pageMyPosts(page, userId, category, keyword, postType, reliabilityStatus);
        return PageBean.page(iPage, iPage.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPost updatePost(ForumPost post) {
        if (post.getId() == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "帖子ID不能为空");
        }
        boolean success = updateById(post);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
        return post;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        // 先删除该帖子下的所有评论
        forumCommentService.deleteCommentsByPostId(id);
        
        // 再删除帖子本身（逻辑删除）
        boolean success = removeById(id);
        if (!success) {
            throw new ServiceException(ResultEnum.DELETE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setTop(Long id, Integer isTop) {
        ForumPost post = getById(id);
        if (post == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "帖子不存在");
        }
        post.setIsTop(isTop);
        boolean success = updateById(post);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEssence(Long id, Integer isEssence) {
        ForumPost post = getById(id);
        if (post == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "帖子不存在");
        }
        post.setIsEssence(isEssence);
        boolean success = updateById(post);
        if (!success) {
            throw new ServiceException(ResultEnum.UPDATE_FAILED);
        }
    }

    @Override
    public PageBean<ForumPost> pagePostsForAdmin(Page<ForumPost> page, String category, String keyword, Integer status, String postType, Integer reliabilityStatus) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        
        // 管理端可以查看所有状态的帖子，不限制status
        
        // 分类筛选
        if (StringUtils.hasText(category)) {
            wrapper.eq(ForumPost::getCategory, category);
        }
        
        // 状态筛选
        if (status != null) {
            wrapper.eq(ForumPost::getStatus, status);
        }
        if (StringUtils.hasText(postType)) {
            wrapper.eq(ForumPost::getPostType, postType);
        }
        if (reliabilityStatus != null) {
            wrapper.eq(ForumPost::getReliabilityStatus, reliabilityStatus);
        }
        
        // 关键词搜索（标题或内容）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ForumPost::getTitle, keyword)
                    .or()
                    .like(ForumPost::getContent, keyword));
        }
        
        // 排序：置顶优先，然后按创建时间倒序
        wrapper.orderByDesc(ForumPost::getIsTop)
                .orderByDesc(ForumPost::getCreatedTime);
        
        IPage<ForumPost> iPage = page(page, wrapper);
        return PageBean.page(iPage, iPage.getRecords());
    }
}


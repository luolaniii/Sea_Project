package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boot.study.bean.LoginInfo;
import com.boot.study.dao.ForumPostAttachmentMapper;
import com.boot.study.entity.ForumPostAttachment;
import com.boot.study.entity.ForumPost;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.ForumPostAttachmentService;
import com.boot.study.service.ForumPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 论坛帖子附件 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForumPostAttachmentServiceImpl extends BaseServiceImpl<ForumPostAttachmentMapper, ForumPostAttachment>
        implements ForumPostAttachmentService {

    private final ForumPostService forumPostService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumPostAttachment addAttachment(ForumPostAttachment attachment) {
        if (LoginInfo.getLoginInfo() == null || LoginInfo.getUserId() == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        attachment.setUploaderId(LoginInfo.getUserId());
        if (attachment.getStatus() == null) {
            attachment.setStatus(1);
        }
        save(attachment);
        return attachment;
    }

    @Override
    public List<ForumPostAttachment> listByPostId(Long postId) {
        return list(new LambdaQueryWrapper<ForumPostAttachment>()
                .eq(ForumPostAttachment::getPostId, postId)
                .eq(ForumPostAttachment::getStatus, 1)
                .orderByDesc(ForumPostAttachment::getCreatedTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long id) {
        if (LoginInfo.getLoginInfo() == null || LoginInfo.getUserId() == null) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        ForumPostAttachment attachment = getById(id);
        if (attachment == null) {
            throw new ServiceException(ResultEnum.PARAM_ERROR.getCode(), "附件不存在");
        }
        ForumPost post = forumPostService.getById(attachment.getPostId());
        String role = LoginInfo.getUserRole();
        Long currentUserId = LoginInfo.getUserId();
        boolean isAdmin = "admin".equalsIgnoreCase(role);
        boolean isPostAuthor = post != null && post.getAuthorId() != null && post.getAuthorId().equals(currentUserId);
        boolean isUploader = attachment.getUploaderId() != null && attachment.getUploaderId().equals(currentUserId);
        if (!isAdmin && !isPostAuthor && !isUploader) {
            log.warn("删除附件权限不足，attachmentId: {}, userId: {}, role: {}", id, currentUserId, role);
            throw new ServiceException(403, "仅附件上传者、帖子作者或管理员可删除附件");
        }
        log.info("删除附件执行，attachmentId: {}, operator: {}, isAdmin: {}, isPostAuthor: {}, isUploader: {}",
                id, currentUserId, isAdmin, isPostAuthor, isUploader);
        boolean success = removeById(id);
        if (!success) {
            throw new ServiceException(ResultEnum.DELETE_FAILED);
        }
    }
}

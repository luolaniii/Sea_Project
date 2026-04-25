package com.boot.study.service;

import com.boot.study.entity.ForumPostAttachment;

import java.util.List;

/**
 * 论坛帖子附件 Service
 */
public interface ForumPostAttachmentService extends BaseService<ForumPostAttachment> {

    ForumPostAttachment addAttachment(ForumPostAttachment attachment);

    List<ForumPostAttachment> listByPostId(Long postId);

    void deleteAttachment(Long id);
}

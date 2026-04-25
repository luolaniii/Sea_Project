package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 论坛帖子附件实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forum_post_attachment")
public class ForumPostAttachment extends BaseDo {

    private Long postId;

    private Long uploaderId;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    /**
     * 状态：0-不可用, 1-可用
     */
    private Integer status;
}

package com.boot.study.api.req.admin;

import com.boot.study.api.req.base.QueryReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 论坛帖子分页查询请求（管理端）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ForumPostPageReq extends QueryReq {

    /**
     * 分类：GENERAL-普通讨论, QUESTION-问题求助, SHARE-经验分享, NEWS-新闻资讯
     */
    private String category;

    /**
     * 关键词（搜索标题和内容）
     */
    private String keyword;

    /**
     * 状态：0-草稿, 1-已发布, 2-已关闭
     */
    private Integer status;

    /**
     * 帖子类型：TOPIC_DISCUSSION-主题讨论, DATA_ANALYSIS-数据分析
     */
    private String postType;

    /**
     * 可靠性状态：0-未评选, 1-评选中, 2-已认证
     */
    private Integer reliabilityStatus;
}


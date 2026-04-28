package com.boot.study.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.entity.ForumPost;
import com.boot.study.response.PageBean;

import java.util.List;

/**
 * 论坛帖子 Service
 */
public interface ForumPostService extends BaseService<ForumPost> {

    /**
     * 分页查询帖子列表
     *
     * @param page     分页参数
     * @param category 分类（可选）
     * @param keyword  关键词（可选，搜索标题和内容）
     * @return 分页结果
     */
    IPage<ForumPost> pagePosts(Page<ForumPost> page, String category, String keyword, String postType, Integer reliabilityStatus, Boolean evaluationCompleted, Boolean reliabilityTrusted);

    /**
     * 分页查询帖子列表（返回PageBean格式）
     *
     * @param page     分页参数
     * @param category 分类（可选）
     * @param keyword  关键词（可选，搜索标题和内容）
     * @return 分页结果
     */
    PageBean<ForumPost> pagePostsWithPageBean(Page<ForumPost> page, String category, String keyword, String postType, Integer reliabilityStatus, Boolean evaluationCompleted, Boolean reliabilityTrusted);

    /**
     * 根据ID查询帖子详情（增加浏览量）
     *
     * @param id 帖子ID
     * @return 帖子详情
     */
    ForumPost getPostDetail(Long id);

    /**
     * 发布帖子
     *
     * @param post 帖子信息
     * @return 保存后的帖子
     */
    ForumPost publishPost(ForumPost post);

    /**
     * 点赞帖子
     *
     * @param postId 帖子ID
     * @return 是否成功
     */
    boolean likePost(Long postId);

    /**
     * 取消点赞帖子
     *
     * @param postId 帖子ID
     * @return 是否成功
     */
    boolean unlikePost(Long postId);

    /**
     * 获取热门帖子列表
     *
     * @param limit 数量限制
     * @return 热门帖子列表
     */
    List<ForumPost> getHotPosts(Integer limit);

    /**
     * 获取置顶帖子列表
     *
     * @return 置顶帖子列表
     */
    List<ForumPost> getTopPosts();

    /**
     * 分页查询当前用户的帖子列表
     *
     * @param page     分页参数
     * @param category 分类（可选）
     * @param keyword  关键词（可选，搜索标题和内容）
     * @param userId   用户ID
     * @return 分页结果
     */
    IPage<ForumPost> pageMyPosts(Page<ForumPost> page, Long userId, String category, String keyword, String postType, Integer reliabilityStatus, Boolean evaluationCompleted, Boolean reliabilityTrusted);

    /**
     * 分页查询当前用户的帖子列表（返回PageBean格式）
     *
     * @param page     分页参数
     * @param category 分类（可选）
     * @param keyword  关键词（可选，搜索标题和内容）
     * @param userId   用户ID
     * @return 分页结果
     */
    PageBean<ForumPost> pageMyPostsWithPageBean(Page<ForumPost> page, Long userId, String category, String keyword, String postType, Integer reliabilityStatus, Boolean evaluationCompleted, Boolean reliabilityTrusted);

    /**
     * 更新帖子
     *
     * @param post 帖子信息
     * @return 更新后的帖子
     */
    ForumPost updatePost(ForumPost post);

    /**
     * 删除帖子
     *
     * @param id 帖子ID
     */
    void deletePost(Long id);

    /**
     * 设置帖子置顶状态
     *
     * @param id  帖子ID
     * @param isTop 是否置顶：1-置顶, 0-取消置顶
     */
    void setTop(Long id, Integer isTop);

    /**
     * 设置帖子精华状态
     *
     * @param id  帖子ID
     * @param isEssence 是否精华：1-精华, 0-取消精华
     */
    void setEssence(Long id, Integer isEssence);

    /**
     * 管理端分页查询帖子列表（包含所有状态）
     *
     * @param page     分页参数
     * @param category 分类（可选）
     * @param keyword  关键词（可选，搜索标题和内容）
     * @param status   状态（可选）
     * @return 分页结果
     */
    PageBean<ForumPost> pagePostsForAdmin(Page<ForumPost> page, String category, String keyword, Integer status, String postType, Integer reliabilityStatus, Boolean evaluationCompleted, Boolean reliabilityTrusted);
}


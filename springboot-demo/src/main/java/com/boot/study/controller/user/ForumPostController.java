package com.boot.study.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.Result;
import com.boot.study.entity.ForumPost;
import com.boot.study.enums.ResultEnum;
import com.boot.study.utils.PathLongParser;
import com.boot.study.response.PageBean;
import com.boot.study.service.ForumPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 论坛帖子接口
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/user/forum-post")
@RequiredArgsConstructor
public class ForumPostController {

    private final ForumPostService forumPostService;

    /**
     * 分页查询帖子列表
     *
     * @param page     页码
     * @param size     每页数量
     * @param category 分类（可选）
     * @param keyword  关键词（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<PageBean<ForumPost>> pagePosts(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String postType,
            @RequestParam(required = false) Integer reliabilityStatus) {
        Page<ForumPost> pageParam = new Page<>(page, size);
        PageBean<ForumPost> pageBean = forumPostService.pagePostsWithPageBean(pageParam, category, keyword, postType, reliabilityStatus);
        return Result.success(pageBean);
    }

    /**
     * 查询帖子详情
     *
     * @param id 帖子ID
     * @return 帖子详情
     */
    @GetMapping("/{id}")
    public Result<ForumPost> getPostDetail(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        ForumPost post = forumPostService.getPostDetail(id);
        return Result.success(post);
    }

    /**
     * 发布帖子
     *
     * @param post 帖子信息
     * @return 保存后的帖子
     */
    @PostMapping
    public Result<ForumPost> publishPost(@RequestBody ForumPost post) {
        ForumPost result = forumPostService.publishPost(post);
        return Result.success(result);
    }

    /**
     * 更新帖子
     *
     * @param post 帖子信息
     * @return 更新结果
     */
    @PutMapping
    public Result<ForumPost> updatePost(@RequestBody ForumPost post) {
        ForumPost result = forumPostService.updatePost(post);
        return Result.success(result);
    }

    /**
     * 删除帖子
     *
     * @param id 帖子ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<?> deletePost(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        forumPostService.deletePost(id);
        return Result.success();
    }

    /**
     * 点赞帖子
     *
     * @param id 帖子ID
     * @return 操作结果
     */
    @PostMapping("/{id}/like")
    public Result<?> likePost(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        forumPostService.likePost(id);
        return Result.success();
    }

    /**
     * 取消点赞帖子
     *
     * @param id 帖子ID
     * @return 操作结果
     */
    @PostMapping("/{id}/unlike")
    public Result<?> unlikePost(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        forumPostService.unlikePost(id);
        return Result.success();
    }

    /**
     * 获取热门帖子
     *
     * @param limit 数量限制（默认10）
     * @return 热门帖子列表
     */
    @GetMapping("/hot")
    public Result<List<ForumPost>> getHotPosts(@RequestParam(defaultValue = "10") Integer limit) {
        List<ForumPost> posts = forumPostService.getHotPosts(limit);
        return Result.success(posts);
    }

    /**
     * 获取置顶帖子
     *
     * @return 置顶帖子列表
     */
    @GetMapping("/top")
    public Result<List<ForumPost>> getTopPosts() {
        List<ForumPost> posts = forumPostService.getTopPosts();
        return Result.success(posts);
    }

    /**
     * 分页查询当前用户的帖子列表
     *
     * @param page     页码
     * @param size     每页数量
     * @param category 分类（可选）
     * @param keyword  关键词（可选）
     * @return 分页结果
     */
    @GetMapping("/my")
    public Result<PageBean<ForumPost>> getMyPosts(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String postType,
            @RequestParam(required = false) Integer reliabilityStatus) {
        Long userId = LoginInfo.getUserId();
        Page<ForumPost> pageParam = new Page<>(page, size);
        PageBean<ForumPost> pageBean = forumPostService.pageMyPostsWithPageBean(pageParam, userId, category, keyword, postType, reliabilityStatus);
        return Result.success(pageBean);
    }
}


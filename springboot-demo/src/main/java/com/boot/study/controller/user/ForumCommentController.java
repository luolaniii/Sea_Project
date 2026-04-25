package com.boot.study.controller.user;

import com.boot.study.bean.Result;
import com.boot.study.entity.ForumComment;
import com.boot.study.enums.ResultEnum;
import com.boot.study.utils.PathLongParser;
import com.boot.study.service.ForumCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 论坛评论接口
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/user/forum-comment")
@RequiredArgsConstructor
public class ForumCommentController {

    private final ForumCommentService forumCommentService;

    /**
     * 根据帖子ID查询评论列表
     *
     * @param postId 帖子ID
     * @return 评论列表（树形结构）
     */
    @GetMapping("/post/{postId}")
    public Result<List<ForumComment>> getCommentsByPostId(@PathVariable("postId") String postIdStr) {
        Long postId = PathLongParser.tryParse(postIdStr);
        if (postId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "帖子ID无效");
        }
        List<ForumComment> comments = forumCommentService.getCommentsByPostId(postId);
        return Result.success(comments);
    }

    /**
     * 添加评论
     *
     * @param comment 评论信息
     * @return 保存后的评论
     */
    @PostMapping
    public Result<ForumComment> addComment(@RequestBody ForumComment comment) {
        ForumComment result = forumCommentService.addComment(comment);
        return Result.success(result);
    }

    /**
     * 更新评论
     *
     * @param comment 评论信息
     * @return 更新结果
     */
    @PutMapping
    public Result<ForumComment> updateComment(@RequestBody ForumComment comment) {
        ForumComment result = forumCommentService.updateComment(comment);
        return Result.success(result);
    }

    /**
     * 删除评论
     *
     * @param id 评论ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteComment(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        forumCommentService.deleteComment(id);
        return Result.success();
    }

    /**
     * 点赞评论
     *
     * @param id 评论ID
     * @return 操作结果
     */
    @PostMapping("/{id}/like")
    public Result<?> likeComment(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        forumCommentService.likeComment(id);
        return Result.success();
    }

    /**
     * 取消点赞评论
     *
     * @param id 评论ID
     * @return 操作结果
     */
    @PostMapping("/{id}/unlike")
    public Result<?> unlikeComment(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        forumCommentService.unlikeComment(id);
        return Result.success();
    }

    /**
     * 根据父评论ID查询回复列表
     *
     * @param parentId 父评论ID
     * @return 回复列表
     */
    @GetMapping("/parent/{parentId}")
    public Result<List<ForumComment>> getRepliesByParentId(@PathVariable("parentId") String parentIdStr) {
        Long parentId = PathLongParser.tryParse(parentIdStr);
        if (parentId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "父评论ID无效");
        }
        List<ForumComment> replies = forumCommentService.getRepliesByParentId(parentId);
        return Result.success(replies);
    }
}


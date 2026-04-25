package com.boot.study.controller.admin;

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
 * 管理端 - 论坛评论管理接口
 * <p>
 * 提供评论的查询功能，管理端可以查看所有评论
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/forum-comment")
@RequiredArgsConstructor
public class ForumCommentAdminController {

    private final ForumCommentService forumCommentService;

    /**
     * 根据帖子ID查询评论列表（管理端）
     * <p>
     * 管理端可以查看所有状态的评论
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
}


package com.boot.study.controller.user;

import com.boot.study.bean.Result;
import com.boot.study.entity.ForumPostEvaluation;
import com.boot.study.enums.ResultEnum;
import com.boot.study.service.ForumPostEvaluationService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 论坛帖子专家评估接口
 */
@RestController
@RequestMapping("/api/user/forum-post-evaluation")
@RequiredArgsConstructor
public class ForumPostEvaluationController {

    private final ForumPostEvaluationService forumPostEvaluationService;

    @PostMapping
    public Result<ForumPostEvaluation> submit(@RequestBody ForumPostEvaluation evaluation) {
        return Result.success(forumPostEvaluationService.submitEvaluation(evaluation));
    }

    @GetMapping("/post/{postId}")
    public Result<List<ForumPostEvaluation>> listByPostId(@PathVariable("postId") String postIdStr) {
        Long postId = PathLongParser.tryParse(postIdStr);
        if (postId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "帖子ID无效");
        }
        return Result.success(forumPostEvaluationService.listByPostId(postId));
    }
}

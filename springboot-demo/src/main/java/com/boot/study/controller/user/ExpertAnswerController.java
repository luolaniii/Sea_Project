package com.boot.study.controller.user;

import com.boot.study.bean.Result;
import com.boot.study.entity.ExpertAnswer;
import com.boot.study.enums.ResultEnum;
import com.boot.study.utils.PathLongParser;
import com.boot.study.service.ExpertAnswerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端 - 专家答疑接口
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/user/expert-answer")
@RequiredArgsConstructor
public class ExpertAnswerController {

    private final ExpertAnswerService expertAnswerService;

    /**
     * 根据帖子ID查询专家回答列表
     *
     * @param postId 帖子ID
     * @return 专家回答列表
     */
    @GetMapping("/post/{postId}")
    public Result<List<ExpertAnswer>> getAnswersByPostId(@PathVariable("postId") String postIdStr) {
        Long postId = PathLongParser.tryParse(postIdStr);
        if (postId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "帖子ID无效");
        }
        List<ExpertAnswer> answers = expertAnswerService.getAnswersByPostId(postId);
        return Result.success(answers);
    }

    /**
     * 根据评论ID查询专家回答列表
     *
     * @param commentId 评论ID
     * @return 专家回答列表
     */
    @GetMapping("/comment/{commentId}")
    public Result<List<ExpertAnswer>> getAnswersByCommentId(@PathVariable("commentId") String commentIdStr) {
        Long commentId = PathLongParser.tryParse(commentIdStr);
        if (commentId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "评论ID无效");
        }
        List<ExpertAnswer> answers = expertAnswerService.getAnswersByCommentId(commentId);
        return Result.success(answers);
    }

    /**
     * 添加专家回答
     *
     * @param answer 回答信息
     * @return 保存后的回答
     */
    @PostMapping
    public Result<ExpertAnswer> addAnswer(@RequestBody ExpertAnswer answer) {
        ExpertAnswer result = expertAnswerService.addAnswer(answer);
        return Result.success(result);
    }

    /**
     * 更新专家回答
     *
     * @param answer 回答信息
     * @return 更新结果
     */
    @PutMapping
    public Result<ExpertAnswer> updateAnswer(@RequestBody ExpertAnswer answer) {
        ExpertAnswer result = expertAnswerService.updateAnswer(answer);
        return Result.success(result);
    }

    /**
     * 删除专家回答
     *
     * @param id 回答ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteAnswer(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        expertAnswerService.deleteAnswer(id);
        return Result.success();
    }

    /**
     * 采纳专家回答
     *
     * @param id 回答ID
     * @return 操作结果
     */
    @PostMapping("/{id}/accept")
    public Result<?> acceptAnswer(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        expertAnswerService.acceptAnswer(id);
        return Result.success();
    }

    /**
     * 点赞专家回答
     *
     * @param id 回答ID
     * @return 操作结果
     */
    @PostMapping("/{id}/like")
    public Result<?> likeAnswer(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        expertAnswerService.likeAnswer(id);
        return Result.success();
    }

    /**
     * 取消点赞专家回答
     *
     * @param id 回答ID
     * @return 操作结果
     */
    @PostMapping("/{id}/unlike")
    public Result<?> unlikeAnswer(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        expertAnswerService.unlikeAnswer(id);
        return Result.success();
    }
}


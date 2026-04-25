package com.boot.study.controller.user;

import com.boot.study.bean.Result;
import com.boot.study.entity.ForumPostVote;
import com.boot.study.enums.ResultEnum;
import com.boot.study.service.ForumPostVoteService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端 - 论坛帖子评选投票接口
 */
@RestController
@RequestMapping("/api/user/forum-post-vote")
@RequiredArgsConstructor
public class ForumPostVoteController {

    private final ForumPostVoteService forumPostVoteService;

    @PostMapping("/post/{postId}")
    public Result<ForumPostVote> vote(@PathVariable("postId") String postIdStr,
                                      @RequestParam Integer voteType) {
        Long postId = PathLongParser.tryParse(postIdStr);
        if (postId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "帖子ID无效");
        }
        return Result.success(forumPostVoteService.vote(postId, voteType));
    }

    @GetMapping("/post/{postId}/summary")
    public Result<Map<String, Object>> summary(@PathVariable("postId") String postIdStr) {
        Long postId = PathLongParser.tryParse(postIdStr);
        if (postId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "帖子ID无效");
        }
        return Result.success(forumPostVoteService.getVoteSummary(postId));
    }
}

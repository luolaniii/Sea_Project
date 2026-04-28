package com.boot.study.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.api.req.admin.ForumPostPageReq;
import com.boot.study.bean.Result;
import com.boot.study.controller.BaseController;
import com.boot.study.entity.ForumPost;
import com.boot.study.enums.ResultEnum;
import com.boot.study.utils.PathLongParser;
import com.boot.study.response.PageBean;
import com.boot.study.service.ForumPostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 论坛帖子管理接口
 * <p>
 * 提供帖子的管理功能，包括分页查询、置顶、精华、删除等操作
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/forum-post")
public class ForumPostAdminController extends BaseController<ForumPostService, ForumPost> {

    /**
     * 帖子分页查询（管理端）
     * <p>
     * 管理端可以查看所有状态的帖子，支持按分类、关键词、状态筛选
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果，包含帖子列表
     */
    @PostMapping("/page")
    public Result<PageBean<ForumPost>> page(@RequestBody ForumPostPageReq req) {
        Page<ForumPost> page = new Page<>(req.getPageNum(), req.getPageSize());
        PageBean<ForumPost> pageBean = baseService.pagePostsForAdmin(
                page,
                req.getCategory(),
                req.getKeyword(),
                req.getStatus(),
                req.getPostType(),
                req.getReliabilityStatus(),
                req.getEvaluationCompleted(),
                req.getReliabilityTrusted()
        );
        return Result.success(pageBean);
    }

    /**
     * 根据ID查询帖子详情（管理端）
     * <p>
     * 管理端查看详情不会增加浏览量
     *
     * @param id 帖子ID
     * @return 帖子详情
     */
    @GetMapping("/{id}")
    @Override
    public Result<ForumPost> getById(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        ForumPost post = baseService.getById(id);
        return Result.success(post);
    }

    /**
     * 设置帖子置顶状态
     *
     * @param id    帖子ID
     * @param isTop 是否置顶：1-置顶, 0-取消置顶
     * @return 操作结果
     */
    @PutMapping("/{id}/top")
    public Result<?> setTop(@PathVariable("id") String idStr, @RequestParam Integer isTop) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        if (isTop == null || (isTop != 0 && isTop != 1)) {
            return Result.fail(400, "isTop参数必须为0或1");
        }
        baseService.setTop(id, isTop);
        return Result.success();
    }

    /**
     * 设置帖子精华状态
     *
     * @param id        帖子ID
     * @param isEssence 是否精华：1-精华, 0-取消精华
     * @return 操作结果
     */
    @PutMapping("/{id}/essence")
    public Result<?> setEssence(@PathVariable("id") String idStr, @RequestParam Integer isEssence) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        if (isEssence == null || (isEssence != 0 && isEssence != 1)) {
            return Result.fail(400, "isEssence参数必须为0或1");
        }
        baseService.setEssence(id, isEssence);
        return Result.success();
    }

    /**
     * 删除帖子（管理端）
     * <p>
     * 删除帖子时会同时删除该帖子下的所有评论
     *
     * @param id 帖子ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Override
    public Result<?> delete(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "ID无效");
        }
        baseService.deletePost(id);
        return Result.success();
    }
}


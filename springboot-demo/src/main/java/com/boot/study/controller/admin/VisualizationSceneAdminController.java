package com.boot.study.controller.admin;

import com.boot.study.api.req.admin.VisualizationScenePageReq;
import com.boot.study.bean.Result;
import com.boot.study.controller.BaseController;
import com.boot.study.entity.VisualizationScene;
import com.boot.study.entity.VisualizationSceneVO;
import com.boot.study.response.PageBean;
import com.boot.study.service.VisualizationSceneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 可视化场景管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/visualization-scene")
public class VisualizationSceneAdminController extends BaseController<VisualizationSceneService, VisualizationScene> {

    /**
     * 场景分页
     *
     * @param req 查询请求
     * @return 分页结果
     */
    @PostMapping("/page")
    public Result<PageBean<VisualizationSceneVO>> page(@RequestBody VisualizationScenePageReq req) {
        PageBean<VisualizationSceneVO> pageBean = baseService.pageListWithVO(req);
        return Result.success(pageBean);
    }

    /**
     * 新增场景
     */
    @PostMapping("/add")
    public Result<VisualizationScene> add(@RequestBody VisualizationScene entity) {
        return save(entity);
    }

    /**
     * 查询所有场景列表
     */
    @GetMapping("/list")
    public Result<List<VisualizationScene>> list() {
        return Result.success(baseService.list());
    }
}
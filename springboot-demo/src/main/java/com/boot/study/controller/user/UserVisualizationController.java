package com.boot.study.controller.user;

import com.boot.study.api.req.user.SceneDataQueryReq;
import com.boot.study.api.req.user.UserScenePageReq;
import com.boot.study.bean.Result;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.entity.VisualizationSceneVO;
import com.boot.study.enums.ResultEnum;
import com.boot.study.response.PageBean;
import com.boot.study.service.VisualizationSceneService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端 - 可视化场景接口控制器
 * <p>
 * 提供用户端访问公开可视化场景的接口，包括：
 * - 获取公开场景列表（分页）
 * - 获取场景详情
 * - 获取场景关联的观测数据
 * <p>
 * Controller层只负责参数接收和结果返回，业务逻辑在Service层实现
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/user/visualization")
@RequiredArgsConstructor
public class UserVisualizationController {

    /**
     * 可视化场景服务
     */
    private final VisualizationSceneService visualizationSceneService;

    /**
     * 获取公开的场景列表（分页）
     *
     * @param req 查询请求
     * @return 分页结果
     */
    @PostMapping("/scene/page")
    public Result<PageBean<VisualizationSceneVO>> getPublicScenes(@RequestBody UserScenePageReq req) {
        PageBean<VisualizationSceneVO> pageBean = visualizationSceneService.getPublicScenes(req);
        return Result.success(pageBean);
    }

    /**
     * 根据ID获取场景详情（仅公开场景）
     *
     * @param id 场景ID
     * @return 场景详情
     */
    @GetMapping("/scene/{id}")
    public Result<VisualizationSceneVO> getSceneById(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "场景ID无效");
        }
        VisualizationSceneVO vo = visualizationSceneService.getPublicSceneById(id);
        return Result.success(vo);
    }

    /**
     * 根据场景ID查询关联的观测数据
     *
     * @param sceneId 场景ID
     * @param req     数据查询请求
     * @return 观测数据列表
     */
    @PostMapping("/scene/{sceneId}/data")
    public Result<PageBean<ObservationDataVO>> getSceneData(
            @PathVariable("sceneId") String sceneIdStr,
            @RequestBody SceneDataQueryReq req) {
        Long sceneId = PathLongParser.tryParse(sceneIdStr);
        if (sceneId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "场景ID无效");
        }
        PageBean<ObservationDataVO> pageBean = visualizationSceneService.getSceneData(sceneId, req);
        return Result.success(pageBean);
    }
}


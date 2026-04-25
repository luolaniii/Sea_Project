package com.boot.study.controller.user;

import com.boot.study.api.req.user.ChartDataQueryReq;
import com.boot.study.api.req.user.UserChartPageReq;
import com.boot.study.bean.Result;
import com.boot.study.entity.ChartConfigVO;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.enums.ResultEnum;
import com.boot.study.response.PageBean;
import com.boot.study.service.ChartConfigService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端 - 图表配置接口控制器
 * <p>
 * 提供用户端访问公开图表配置的接口，包括：
 * - 获取公开图表列表（分页）
 * - 获取图表详情
 * - 获取图表关联的观测数据
 * <p>
 * Controller层只负责参数接收和结果返回，业务逻辑在Service层实现
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/user/chart")
@RequiredArgsConstructor
public class UserChartController {

    /**
     * 图表配置服务
     */
    private final ChartConfigService chartConfigService;

    /**
     * 获取公开的图表列表（分页）
     *
     * @param req 查询请求
     * @return 分页结果
     */
    @PostMapping("/page")
    public Result<PageBean<ChartConfigVO>> getPublicCharts(@RequestBody UserChartPageReq req) {
        PageBean<ChartConfigVO> pageBean = chartConfigService.getPublicCharts(req);
        return Result.success(pageBean);
    }

    /**
     * 根据ID获取图表详情（仅公开图表）
     *
     * @param id 图表ID
     * @return 图表详情
     */
    @GetMapping("/{id}")
    public Result<ChartConfigVO> getChartById(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        if (id == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "图表ID无效");
        }
        ChartConfigVO vo = chartConfigService.getPublicChartById(id);
        return Result.success(vo);
    }

    /**
     * 根据图表ID查询关联的观测数据
     *
     * @param chartId 图表ID
     * @param req     数据查询请求
     * @return 观测数据列表
     */
    @PostMapping("/{chartId}/data")
    public Result<PageBean<ObservationDataVO>> getChartData(
            @PathVariable("chartId") String chartIdStr,
            @RequestBody ChartDataQueryReq req) {
        Long chartId = PathLongParser.tryParse(chartIdStr);
        if (chartId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "图表ID无效");
        }
        PageBean<ObservationDataVO> pageBean = chartConfigService.getChartData(chartId, req);
        return Result.success(pageBean);
    }
}


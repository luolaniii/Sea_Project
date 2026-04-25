package com.boot.study.controller.admin;

import com.boot.study.api.req.admin.ChartConfigPageReq;
import com.boot.study.bean.Result;
import com.boot.study.controller.BaseController;
import com.boot.study.entity.ChartConfig;
import com.boot.study.entity.ChartConfigVO;
import com.boot.study.response.PageBean;
import com.boot.study.service.ChartConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 图表配置管理接口
 * <p>
 * 提供图表配置的增删改查功能，包括分页查询、新增、列表查询等
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/chart-config")
public class ChartConfigAdminController extends BaseController<ChartConfigService, ChartConfig> {

    /**
     * 图表配置分页查询
     * <p>
     * 根据查询条件分页查询图表配置，返回包含枚举描述的VO对象列表
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果，包含图表配置VO列表
     */
    @PostMapping("/page")
    public Result<PageBean<ChartConfigVO>> page(@RequestBody ChartConfigPageReq req) {
        PageBean<ChartConfigVO> pageBean = baseService.pageListWithVO(req);
        return Result.success(pageBean);
    }

    /**
     * 新增图表配置
     * <p>
     * 创建新的图表配置记录
     *
     * @param entity 图表配置实体对象
     * @return 保存结果，包含保存后的图表配置对象
     */
    @PostMapping("/add")
    public Result<ChartConfig> add(@RequestBody ChartConfig entity) {
        return save(entity);
    }

    /**
     * 查询所有图表配置列表
     * <p>
     * 查询所有图表配置，不分页，用于下拉选择等场景
     *
     * @return 所有图表配置列表
     */
    @GetMapping("/list")
    public Result<List<ChartConfig>> list() {
        return Result.success(baseService.list());
    }
}
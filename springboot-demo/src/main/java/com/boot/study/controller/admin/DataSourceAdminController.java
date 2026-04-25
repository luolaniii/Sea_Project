package com.boot.study.controller.admin;

import com.boot.study.api.req.admin.DataSourcePageReq;
import com.boot.study.bean.Result;
import com.boot.study.controller.BaseController;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.DataSourceVO;
import com.boot.study.response.PageBean;
import com.boot.study.service.DataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 数据源管理接口
 * <p>
 * 提供数据源的增删改查功能，包括分页查询、新增、列表查询等
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/data-source")
public class DataSourceAdminController extends BaseController<DataSourceService, DataSource> {

    /**
     * 数据源分页查询
     * <p>
     * 根据查询条件分页查询数据源，返回包含枚举描述的VO对象列表
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果，包含数据源VO列表
     */
    @PostMapping("/page")
    public Result<PageBean<DataSourceVO>> page(@RequestBody DataSourcePageReq req) {
        PageBean<DataSourceVO> pageBean = baseService.pageListWithVO(req);
        return Result.success(pageBean);
    }

    /**
     * 新增数据源
     * <p>
     * 创建新的数据源记录
     *
     * @param entity 数据源实体对象
     * @return 保存结果，包含保存后的数据源对象
     */
    @PostMapping("/add")
    public Result<DataSource> add(@RequestBody DataSource entity) {
        return save(entity);
    }

    /**
     * 查询所有数据源列表
     * <p>
     * 查询所有数据源，不分页，用于下拉选择等场景
     *
     * @return 所有数据源列表
     */
    @GetMapping("/list")
    public Result<List<DataSource>> list() {
        return Result.success(baseService.list());
    }
}
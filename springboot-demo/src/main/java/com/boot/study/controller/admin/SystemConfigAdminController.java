package com.boot.study.controller.admin;

import com.boot.study.api.req.admin.SystemConfigPageReq;
import com.boot.study.bean.Result;
import com.boot.study.controller.BaseController;
import com.boot.study.entity.SystemConfig;
import com.boot.study.entity.SystemConfigVO;
import com.boot.study.response.PageBean;
import com.boot.study.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 系统配置管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/system-config")
public class SystemConfigAdminController extends BaseController<SystemConfigService, SystemConfig> {

    /**
     * 系统配置分页
     *
     * @param req 查询请求
     * @return 分页结果
     */
    @PostMapping("/page")
    public Result<PageBean<SystemConfigVO>> page(@RequestBody SystemConfigPageReq req) {
        PageBean<SystemConfigVO> pageBean = baseService.pageListWithVO(req);
        return Result.success(pageBean);
    }

    /**
     * 新增系统配置
     */
    @PostMapping("/add")
    public Result<SystemConfig> add(@RequestBody SystemConfig entity) {
        return save(entity);
    }

    /**
     * 查询所有系统配置列表
     */
    @GetMapping("/list")
    public Result<List<SystemConfig>> list() {
        return Result.success(baseService.list());
    }
}
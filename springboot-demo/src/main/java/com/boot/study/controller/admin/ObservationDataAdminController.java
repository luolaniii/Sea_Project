package com.boot.study.controller.admin;

import com.boot.study.api.req.admin.ObservationDataPageReq;
import com.boot.study.bean.Result;
import com.boot.study.controller.BaseController;
import com.boot.study.entity.ObservationData;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.response.PageBean;
import com.boot.study.service.ObservationDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 观测数据管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/observation-data")
public class ObservationDataAdminController extends BaseController<ObservationDataService, ObservationData> {

    /**
     * 观测数据分页
     */
    @PostMapping("/page")
    public Result<PageBean<ObservationDataVO>> page(@RequestBody ObservationDataPageReq req) {
        PageBean<ObservationDataVO> pageBean = baseService.pageListWithRelation(req);
        return Result.success(pageBean);
    }

    /**
     * 新增观测数据
     */
    @PostMapping("/add")
    public Result<ObservationData> add(@RequestBody ObservationData entity) {
        return save(entity);
    }

    /**
     * 查询所有观测数据列表
     */
    @GetMapping("/list")
    public Result<List<ObservationData>> list() {
        return Result.success(baseService.list());
    }
}
package com.boot.study.controller.admin;

import com.boot.study.bean.Result;
import com.boot.study.controller.BaseController;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.service.ObservationDataTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 - 观测数据类型管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/observation-data-type")
public class ObservationDataTypeAdminController extends BaseController<ObservationDataTypeService, ObservationDataType> {

    /**
     * 查询所有观测数据类型列表
     */
    @GetMapping("/list")
    public Result<List<ObservationDataType>> list() {
        return Result.success(baseService.list());
    }
}



package com.boot.study.controller.admin;

import com.boot.study.controller.BaseController;
import com.boot.study.entity.DataFile;
import com.boot.study.service.DataFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 数据文件管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/data-file")
public class DataFileAdminController extends BaseController<DataFileService, DataFile> {
}



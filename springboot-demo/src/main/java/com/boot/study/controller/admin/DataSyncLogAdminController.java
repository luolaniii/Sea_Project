package com.boot.study.controller.admin;

import com.boot.study.controller.BaseController;
import com.boot.study.entity.DataSyncLog;
import com.boot.study.service.DataSyncLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 数据同步日志管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/data-sync-log")
public class DataSyncLogAdminController extends BaseController<DataSyncLogService, DataSyncLog> {
}



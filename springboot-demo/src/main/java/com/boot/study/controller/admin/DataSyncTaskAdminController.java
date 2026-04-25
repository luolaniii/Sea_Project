package com.boot.study.controller.admin;

import com.boot.study.controller.BaseController;
import com.boot.study.entity.DataSyncTask;
import com.boot.study.service.DataSyncTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 数据同步任务管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/data-sync-task")
public class DataSyncTaskAdminController extends BaseController<DataSyncTaskService, DataSyncTask> {
}



package com.boot.study.service.impl;

import com.boot.study.dao.DataSyncTaskMapper;
import com.boot.study.entity.DataSyncTask;
import com.boot.study.service.DataSyncTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据同步任务表 Service 实现
 */
@Slf4j
@Service
public class DataSyncTaskServiceImpl extends BaseServiceImpl<DataSyncTaskMapper, DataSyncTask> implements DataSyncTaskService {
}



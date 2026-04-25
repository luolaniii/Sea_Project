package com.boot.study.service.impl;

import com.boot.study.dao.DataSyncLogMapper;
import com.boot.study.entity.DataSyncLog;
import com.boot.study.service.DataSyncLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据同步日志表 Service 实现
 */
@Slf4j
@Service
public class DataSyncLogServiceImpl extends BaseServiceImpl<DataSyncLogMapper, DataSyncLog> implements DataSyncLogService {
}



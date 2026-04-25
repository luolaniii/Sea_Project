package com.boot.study.service.impl;

import com.boot.study.dao.DataFileMapper;
import com.boot.study.entity.DataFile;
import com.boot.study.service.DataFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据文件表 Service 实现
 */
@Slf4j
@Service
public class DataFileServiceImpl extends BaseServiceImpl<DataFileMapper, DataFile> implements DataFileService {
}



package com.boot.study.service.impl;

import com.boot.study.dao.ObservationDataTypeMapper;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.service.ObservationDataTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 观测数据类型表 Service 实现
 */
@Slf4j
@Service
public class ObservationDataTypeServiceImpl extends BaseServiceImpl<ObservationDataTypeMapper, ObservationDataType> implements ObservationDataTypeService {
}



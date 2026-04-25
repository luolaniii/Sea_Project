package com.boot.study.service.impl;

import com.boot.study.dao.SceneDataRelationMapper;
import com.boot.study.entity.SceneDataRelation;
import com.boot.study.service.SceneDataRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 场景数据关联表 Service 实现
 */
@Slf4j
@Service
public class SceneDataRelationServiceImpl extends BaseServiceImpl<SceneDataRelationMapper, SceneDataRelation> implements SceneDataRelationService {
}



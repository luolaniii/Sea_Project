package com.boot.study.controller.admin;

import com.boot.study.controller.BaseController;
import com.boot.study.entity.SceneDataRelation;
import com.boot.study.service.SceneDataRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 场景数据关联管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/scene-data-relation")
public class SceneDataRelationAdminController extends BaseController<SceneDataRelationService, SceneDataRelation> {
}



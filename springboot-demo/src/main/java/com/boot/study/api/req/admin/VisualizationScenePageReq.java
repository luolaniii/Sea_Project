package com.boot.study.api.req.admin;

import com.boot.study.api.req.base.QueryReq;
import com.boot.study.enums.PublicFlagEnum;
import com.boot.study.enums.SceneTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可视化场景分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VisualizationScenePageReq extends QueryReq {

    /**
     * 场景名称，模糊匹配
     */
    private String sceneName;

    /**
     * 场景类型
     */
    private SceneTypeEnum sceneType;

    /**
     * 是否公开
     */
    private PublicFlagEnum isPublic;
}


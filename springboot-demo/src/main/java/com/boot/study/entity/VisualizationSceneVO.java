package com.boot.study.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可视化场景VO，包含枚举desc字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VisualizationSceneVO extends VisualizationScene {
    
    /**
     * 场景类型描述
     */
    private String sceneTypeDesc;
    
    /**
     * 场景类型编码
     */
    private String sceneTypeCode;
    
    /**
     * 是否公开描述
     */
    private String isPublicDesc;
    
    /**
     * 是否公开编码
     */
    private Integer isPublicCode;
}


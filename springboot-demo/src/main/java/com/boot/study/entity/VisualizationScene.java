package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.study.enums.PublicFlagEnum;
import com.boot.study.enums.SceneTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可视化场景表实体，对应表：visualization_scene
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visualization_scene")
public class VisualizationScene extends BaseDo {

    /**
     * 场景名称
     */
    private String sceneName;

    /**
     * 场景类型
     */
    private SceneTypeEnum sceneType;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 是否公开：0-私有, 1-公开
     */
    private PublicFlagEnum isPublic;

    /**
     * 场景配置（JSON格式）
     */
    private String configJson;

    /**
     * 缩略图URL
     */
    private String thumbnail;

    /**
     * 描述
     */
    private String description;

    /**
     * 查看次数
     */
    private Integer viewCount;
}



package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.study.enums.SceneDataTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 场景数据关联表实体，对应表：scene_data_relation
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scene_data_relation")
public class SceneDataRelation extends BaseDo {

    /**
     * 场景ID
     */
    private Long sceneId;

    /**
     * 数据类型：DATA_SOURCE-数据源, DATA-观测数据, FILE-文件
     */
    private SceneDataTypeEnum dataType;

    /**
     * 数据ID
     */
    private Long dataId;

    /**
     * 显示配置（JSON格式）
     */
    private String displayConfig;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}



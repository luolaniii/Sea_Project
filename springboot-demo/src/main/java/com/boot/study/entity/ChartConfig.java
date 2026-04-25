package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.study.enums.ChartTypeEnum;
import com.boot.study.enums.PublicFlagEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图表配置表实体
 * <p>
 * 对应数据库表：chart_config
 * 用于存储图表的配置信息，包括图表类型、数据查询配置、ECharts配置等
 *
 * @author study
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chart_config")
public class ChartConfig extends BaseDo {

    /**
     * 图表名称
     */
    private String chartName;

    /**
     * 图表类型
     */
    private ChartTypeEnum chartType;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 数据查询配置（JSON格式）
     */
    private String dataQueryConfig;

    /**
     * ECharts配置（JSON格式）
     */
    private String echartsConfig;

    /**
     * 是否公开
     */
    private PublicFlagEnum isPublic;

    /**
     * 描述
     */
    private String description;
}



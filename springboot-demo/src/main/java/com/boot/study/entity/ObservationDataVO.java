package com.boot.study.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 观测数据VO，包含枚举desc字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ObservationDataVO extends ObservationData {
    
    /**
     * 质量标志描述
     */
    private String qualityFlagDesc;
    
    /**
     * 数据源类型描述
     */
    private String dataSourceTypeDesc;
    
    /**
     * 数据源名称
     */
    private String dataSourceName;
    
    /**
     * 数据类型名称
     */
    private String dataTypeName;
    
    /**
     * 数据类型编码
     */
    private String dataTypeCode;
    
    /**
     * 数据类型单位
     */
    private String dataTypeUnit;
}


package com.boot.study.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置VO，包含枚举desc字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfigVO extends SystemConfig {
    
    /**
     * 配置类型描述
     */
    private String configTypeDesc;
    
    /**
     * 配置类型编码
     */
    private String configTypeCode;
}


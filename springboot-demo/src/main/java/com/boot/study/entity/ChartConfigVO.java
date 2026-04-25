package com.boot.study.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图表配置VO（视图对象）
 * <p>
 * 继承ChartConfig实体类，扩展了枚举类型的描述字段
 * 用于前端展示，将枚举类型转换为可读的描述信息
 *
 * @author study
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChartConfigVO extends ChartConfig {
    
    /**
     * 图表类型描述
     */
    private String chartTypeDesc;
    
    /**
     * 图表类型编码
     */
    private String chartTypeCode;
    
    /**
     * 是否公开描述
     */
    private String isPublicDesc;
    
    /**
     * 是否公开编码
     */
    private Integer isPublicCode;
}


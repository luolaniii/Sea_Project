package com.boot.study.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据源VO（视图对象）
 * <p>
 * 继承DataSource实体类，扩展了枚举类型的描述字段
 * 用于前端展示，将枚举类型转换为可读的描述信息
 *
 * @author study
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DataSourceVO extends DataSource {
    
    /**
     * 数据源类型描述
     */
    private String sourceTypeDesc;
    
    /**
     * 状态描述
     */
    private String statusDesc;
}


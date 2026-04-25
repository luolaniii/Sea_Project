package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 观测数据类型表实体，对应表：observation_data_type
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("observation_data_type")
public class ObservationDataType extends BaseDo {

    /**
     * 类型编码
     */
    private String typeCode;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 单位
     */
    private String unit;

    /**
     * 数据格式：FLOAT、INTEGER
     */
    private String dataFormat;

    /**
     * 最小值
     */
    private java.math.BigDecimal minValue;

    /**
     * 最大值
     */
    private java.math.BigDecimal maxValue;

    /**
     * 描述
     */
    private String description;

    /**
     * 变化率阈值（每小时）
     */
    private java.math.BigDecimal rateOfChangeLimit;

    /**
     * 尖峰检测阈值（与均值偏差倍数）
     */
    private java.math.BigDecimal spikeThreshold;

    /**
     * 持续性检测阈值（连续相同值数量）
     */
    private Integer persistenceLimit;

    /**
     * NOAA文件中的变量名（如WSPD、ATMP、SAL等）
     */
    private String noaaVariableName;
}



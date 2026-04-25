package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 观测数据表实体，对应表：observation_data
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("observation_data")
public class ObservationData extends BaseDo {

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * 数据类型ID
     */
    private Long dataTypeId;

    /**
     * 观测时间
     */
    private LocalDateTime observationTime;

    /**
     * 数据值
     */
    private BigDecimal dataValue;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 深度（米）
     */
    private BigDecimal depth;

    /**
     * 质量标志：GOOD、QUESTIONABLE、BAD
     */
    private String qualityFlag;

    /**
     * 来源文件ID
     */
    private Long sourceFileId;

    /**
     * API返回的原始数据ID
     */
    private String apiDataId;

    /**
     * 备注
     */
    private String remark;
}



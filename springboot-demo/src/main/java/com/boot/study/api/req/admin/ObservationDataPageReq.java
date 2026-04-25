package com.boot.study.api.req.admin;

import com.boot.study.api.req.base.QueryReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 观测数据分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ObservationDataPageReq extends QueryReq {

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * 数据类型ID
     */
    private Long dataTypeId;

    /**
     * 数据类型ID集合（支持一次查询多种数据类型，例如 TEMPERATURE/SALINITY 等）
     */
    private List<Long> dataTypeIds;

    /**
     * 质量标志
     */
    private String qualityFlag;

    /**
     * 观测时间起
     */
    private LocalDateTime startTime;

    /**
     * 观测时间止
     */
    private LocalDateTime endTime;

    /**
     * 经度范围（最小值）
     */
    private Double minLongitude;

    /**
     * 经度范围（最大值）
     */
    private Double maxLongitude;

    /**
     * 纬度范围（最小值）
     */
    private Double minLatitude;

    /**
     * 纬度范围（最大值）
     */
    private Double maxLatitude;
}


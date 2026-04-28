package com.boot.study.api.req.admin;

import com.boot.study.api.req.base.QueryReq;
import com.boot.study.enums.ChartTypeEnum;
import com.boot.study.enums.PublicFlagEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 图表配置分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChartConfigPageReq extends QueryReq {

    /**
     * 图表名称，模糊匹配
     */
    private String chartName;

    /**
     * 图表类型
     */
    private ChartTypeEnum chartType;

    /**
     * 是否公开
     */
    private PublicFlagEnum isPublic;

    /**
     * 站点名称/站点ID关键词（匹配数据源名称、站点ID）
     */
    private String stationKeyword;

    /**
     * 站点类型（数据源类型）：NOAA、ERDDAP、CUSTOM
     */
    private String stationType;

    /**
     * 海域分类（按数据源经纬度推断）
     */
    private String oceanRegion;

    /**
     * 站点归属/项目/Owner 关键词
     */
    private String programOwner;

    /**
     * 观测数据类型编码（如 WTMP、WAVE_HEIGHT）
     */
    private String dataTypeCode;

    /**
     * 经度范围
     */
    private BigDecimal minLongitude;
    private BigDecimal maxLongitude;

    /**
     * 纬度范围
     */
    private BigDecimal minLatitude;
    private BigDecimal maxLatitude;
}


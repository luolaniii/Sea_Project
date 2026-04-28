package com.boot.study.api.req.admin;

import com.boot.study.api.req.base.QueryReq;
import com.boot.study.enums.PublicFlagEnum;
import com.boot.study.enums.SceneTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 可视化场景分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VisualizationScenePageReq extends QueryReq {

    /**
     * 场景名称，模糊匹配
     */
    private String sceneName;

    /**
     * 场景类型
     */
    private SceneTypeEnum sceneType;

    /**
     * 是否公开
     */
    private PublicFlagEnum isPublic;

    /**
     * 站点名 / 站点ID 关键词
     */
    private String stationKeyword;

    /**
     * 推断站点类型：MET/WAVE/OCEAN/DART/CURRENT/WATER_LEVEL
     */
    private String stationType;

    /**
     * 推断海域：PACIFIC/ATLANTIC/INDIAN/ARCTIC/SOUTHERN/GULF_OF_MEXICO/GREAT_LAKES
     */
    private String oceanRegion;

    /**
     * 观测数据类型编码，如 WVHT / WTMP
     */
    private String dataTypeCode;

    private BigDecimal minLongitude;

    private BigDecimal maxLongitude;

    private BigDecimal minLatitude;

    private BigDecimal maxLatitude;
}


package com.boot.study.api.req.user;

import com.boot.study.api.req.base.QueryReq;
import com.boot.study.enums.SceneTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 用户端 - 场景分页查询请求（只查询公开场景）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserScenePageReq extends QueryReq {

    /**
     * 场景名称，模糊匹配
     */
    private String sceneName;

    /**
     * 场景类型
     */
    private SceneTypeEnum sceneType;

    /**
     * 站点名称/站点ID关键词
     */
    private String stationKeyword;

    /**
     * 站点类型（MET/WAVE/OCEAN/DART/CURRENT/WATER_LEVEL 等）
     */
    private String stationType;

    /**
     * 海域分类（按数据源经纬度推断）：PACIFIC / ATLANTIC / GULF_OF_MEXICO 等
     */
    private String oceanRegion;

    /**
     * 站点归属/项目/Owner 关键词
     */
    private String programOwner;

    /**
     * 观测数据类型编码（如 WTMP、WVHT、PRES）
     */
    private String dataTypeCode;

    /**
     * 经纬度范围（按场景绑定的数据源坐标筛选）
     */
    private BigDecimal minLongitude;
    private BigDecimal maxLongitude;
    private BigDecimal minLatitude;
    private BigDecimal maxLatitude;
}


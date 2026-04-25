package com.boot.study.api.req.user;

import com.boot.study.api.req.base.QueryReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户端 - 场景数据查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SceneDataQueryReq extends QueryReq {

    /**
     * 场景ID
     */
    private Long sceneId;

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * 数据类型ID
     */
    private Long dataTypeId;

    /**
     * 数据类型编码列表（例如 ["TEMPERATURE","SALINITY"]）
     * <p>
     * 可用于一次查询多种数据类型（服务端会转换为 dataTypeIds 进行过滤）
     */
    private List<String> typeCodes;

    /**
     * 观测时间起（字符串，后端自行解析）
     * 支持格式示例：
     *  - 2024-01-01 00:00:00
     *  - 2024-01-01T00:00:00
     */
    private String startTime;

    /**
     * 观测时间止（字符串，后端自行解析）
     */
    private String endTime;

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


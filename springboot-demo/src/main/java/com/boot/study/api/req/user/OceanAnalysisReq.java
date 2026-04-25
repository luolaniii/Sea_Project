package com.boot.study.api.req.user;

import com.boot.study.api.req.base.QueryReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 海洋环境分析请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OceanAnalysisReq extends QueryReq {

    /**
     * 数据源ID（站点）
     */
    private Long dataSourceId;

    /**
     * 查询起始时间（可选，格式：yyyy-MM-dd HH:mm:ss 或 ISO）
     */
    private String startTime;

    /**
     * 查询结束时间（可选，格式：yyyy-MM-dd HH:mm:ss 或 ISO）
     */
    private String endTime;

    /**
     * 无 startTime 时回看小时数
     */
    private Integer historyHours = 24;
}


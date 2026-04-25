package com.boot.study.api.req.user;

import com.boot.study.api.req.base.QueryReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 海洋多站点对比分析请求
 * <p>
 * 对应开题报告"多源数据融合"需求：同时对比多个数据源（站点）的海况，
 * 识别空间异常（某站相对其他站显著偏高/偏低）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OceanComparisonReq extends QueryReq {

    /**
     * 参与对比的站点 ID 列表（至少 2 个）
     */
    private List<Long> dataSourceIds;

    /**
     * 查询起始时间（可选）
     */
    private String startTime;

    /**
     * 查询结束时间（可选）
     */
    private String endTime;

    /**
     * 无 startTime 时回看小时数（默认 24）
     */
    private Integer historyHours = 24;
}

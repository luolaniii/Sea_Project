package com.boot.study.api.req.user;

import com.boot.study.api.req.base.QueryReq;
import com.boot.study.enums.ChartTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户端 - 图表分页查询请求（只查询公开图表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserChartPageReq extends QueryReq {

    /**
     * 图表名称，模糊匹配
     */
    private String chartName;

    /**
     * 图表类型
     */
    private ChartTypeEnum chartType;
}


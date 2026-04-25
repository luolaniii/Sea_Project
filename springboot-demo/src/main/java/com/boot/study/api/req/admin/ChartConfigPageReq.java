package com.boot.study.api.req.admin;

import com.boot.study.api.req.base.QueryReq;
import com.boot.study.enums.ChartTypeEnum;
import com.boot.study.enums.PublicFlagEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}


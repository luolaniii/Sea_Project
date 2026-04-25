package com.boot.study.api.req.admin;

import com.boot.study.api.req.base.QueryReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据源分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DataSourcePageReq extends QueryReq {

    /**
     * 按名称模糊查询
     */
    private String sourceName;

    /**
     * 数据源类型
     */
    private String sourceType;

    /**
     * 状态：0-禁用, 1-启用
     */
    private Integer status;
}


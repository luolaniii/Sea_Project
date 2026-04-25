package com.boot.study.api.req.admin;

import com.boot.study.api.req.base.QueryReq;
import com.boot.study.enums.ConfigTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfigPageReq extends QueryReq {

    /**
     * 配置键（模糊匹配）
     */
    private String configKey;

    /**
     * 配置分组
     */
    private String configGroup;

    /**
     * 配置类型
     */
    private ConfigTypeEnum configType;

    /**
     * 是否系统配置
     */
    private Boolean isSystem;
}


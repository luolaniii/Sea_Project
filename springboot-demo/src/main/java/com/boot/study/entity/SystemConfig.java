package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.study.enums.ConfigTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置表实体，对应表：system_config
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfig extends BaseDo {

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置类型
     */
    private ConfigTypeEnum configType;

    /**
     * 配置分组
     */
    private String configGroup;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否系统配置：0-用户配置, 1-系统配置
     */
    private Boolean isSystem;
}



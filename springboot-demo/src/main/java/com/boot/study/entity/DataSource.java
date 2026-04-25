package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据源表实体
 * <p>
 * 对应数据库表：data_source
 * 用于存储外部数据源的配置信息，支持NOAA、ERDDAP、CUSTOM等类型
 *
 * @author study
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_source")
public class DataSource extends BaseDo {

    /**
     * 数据源名称
     */
    private String sourceName;

    /**
     * 数据源类型：NOAA、ERDDAP、CUSTOM
     */
    private String sourceType;

    /**
     * API地址
     */
    private String apiUrl;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 配置信息（JSON格式）
     */
    private String configJson;

    /**
     * 站点编号（如TIBC1、WIWF1）
     */
    private String stationId;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 采集文件后缀（逗号分隔，如：txt,ocean,rain）
     */
    private String fileSuffixes;

    /**
     * 是否自动同步：0-否，1-是
     */
    private Integer autoSync;

    /**
     * 同步间隔（分钟）
     */
    private Integer syncIntervalMinutes;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;
}



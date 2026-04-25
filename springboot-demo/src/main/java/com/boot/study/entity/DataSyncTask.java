package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.study.enums.DataSyncTaskStatusEnum;
import com.boot.study.enums.DataSyncTaskTypeEnum;
import com.boot.study.enums.SyncFrequencyEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据同步任务表实体，对应表：data_sync_task
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_sync_task")
public class DataSyncTask extends BaseDo {

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 数据源ID
     */
    private Long dataSourceId;

    /**
     * 任务类型
     */
    private DataSyncTaskTypeEnum taskType;

    /**
     * 同步频率
     */
    private SyncFrequencyEnum syncFrequency;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 状态
     */
    private DataSyncTaskStatusEnum status;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 下次同步时间
     */
    private LocalDateTime nextSyncTime;

    /**
     * 同步次数
     */
    private Integer syncCount;

    /**
     * 成功次数
     */
    private Integer successCount;

    /**
     * 失败次数
     */
    private Integer failCount;

    /**
     * 任务配置（JSON格式）
     */
    private String configJson;

    /**
     * 描述
     */
    private String description;
}



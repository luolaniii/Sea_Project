package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boot.study.enums.DataSyncLogStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据同步日志表实体，对应表：data_sync_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_sync_log")
public class DataSyncLog extends BaseDo {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 状态
     */
    private DataSyncLogStatusEnum status;

    /**
     * 同步数据条数
     */
    private Integer dataCount;

    /**
     * 耗时（毫秒）
     */
    private Integer duration;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 日志详情（JSON格式）
     */
    private String logDetail;
}



package com.boot.study.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 专家申请记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("expert_application")
public class ExpertApplication extends BaseDo {

    /** 申请人 ID */
    private Long userId;

    private String realName;

    private String organization;

    private String profession;

    /** 擅长领域 - 逗号分隔 */
    private String expertiseTags;

    private String applicationReason;

    /** PENDING / APPROVED / REJECTED */
    private String status;

    private Long reviewerId;

    private String reviewRemark;

    private LocalDateTime reviewedAt;
}

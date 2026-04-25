package com.boot.study.entity;

import lombok.Data;

import java.util.List;

@Data
public class OceanAbnormalResultVO {
    private Boolean level1Alarm;
    private Boolean level2Alarm;
    private Boolean tempStaticAbnormal;
    private Boolean tideStaticAbnormal;
    private Boolean waveStaticAbnormal;
    private Boolean windStaticAbnormal;
    private Boolean tempDynamicAbnormal;
    private Boolean tideDynamicAbnormal;
    private Boolean waveDynamicAbnormal;
    private Boolean windDynamicAbnormal;
    private String ref15mStatus;
    private String confidence;
    private List<String> degradeReason;
}


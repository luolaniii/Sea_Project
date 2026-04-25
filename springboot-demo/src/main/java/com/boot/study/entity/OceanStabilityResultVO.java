package com.boot.study.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OceanStabilityResultVO {
    private BigDecimal stabilityIndex;
    private String level;
    private BigDecimal tempRate;
    private BigDecimal tideRate;
    private BigDecimal waveRate;
    private BigDecimal windRate;
    private String confidence;
    private List<String> degradeReason;
}


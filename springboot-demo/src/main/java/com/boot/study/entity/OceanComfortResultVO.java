package com.boot.study.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OceanComfortResultVO {
    private BigDecimal score;
    private String level;
    private String suggestion;
    private String confidence;
    private List<String> degradeReason;
}


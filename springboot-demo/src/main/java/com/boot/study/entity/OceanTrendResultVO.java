package com.boot.study.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OceanTrendResultVO {
    private BigDecimal temperaturePredict;
    private String temperatureTrend;
    private BigDecimal seaLevelPredict;
    private String seaLevelTrend;
    private String confidence;
    private List<String> degradeReason;
}


package com.boot.study.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OceanCompositeResultVO {
    private LocalDateTime currentTime;
    private BigDecimal temperature;
    private BigDecimal salinity;
    private BigDecimal seaLevel;
    private BigDecimal waveHeight;
    private BigDecimal windSpeed;
    private OceanAbnormalResultVO abnormal;
    private OceanStabilityResultVO stability;
    private OceanComfortResultVO comfort;
    private OceanTrendResultVO trend;
    private List<SeriesPointVO> series;

    @Data
    public static class SeriesPointVO {
        private LocalDateTime time;
        private BigDecimal temperature;
        private BigDecimal salinity;
        private BigDecimal seaLevel;
        private BigDecimal waveHeight;
        private BigDecimal windSpeed;
    }
}


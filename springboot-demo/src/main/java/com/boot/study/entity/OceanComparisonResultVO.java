package com.boot.study.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 多站点对比分析结果 VO
 * <p>
 * Module I - 解决单站点分析的局限，支持空间对比、异常检测、多源融合。
 */
@Data
public class OceanComparisonResultVO {

    /**
     * 每个站点的摘要信息
     */
    private List<StationSummary> stations;

    /**
     * 参与对比的关键指标汇总（区间、均值、方差）
     */
    private ComparativeMetrics metrics;

    /**
     * 空间异常检测：某站相对于群体显著偏离
     */
    private List<SpatialAnomaly> anomalies;

    /**
     * 置信度标签：HIGH/MEDIUM/LOW
     */
    private String confidence;

    /**
     * 降级原因（某些站点数据缺失等）
     */
    private List<String> degradeReason;

    /**
     * 单站点摘要
     */
    @Data
    public static class StationSummary {
        private Long dataSourceId;
        private String sourceName;
        private String stationId;
        private BigDecimal longitude;
        private BigDecimal latitude;
        private BigDecimal temperature;
        private BigDecimal salinity;
        private BigDecimal seaLevel;
        private BigDecimal waveHeight;
        private BigDecimal windSpeed;
        private BigDecimal stabilityIndex;
        /** 综合异常是否触发 */
        private Boolean abnormal;
    }

    /**
     * 群体指标：区间、均值、方差（用于展示"北站海况比南站稳定"之类）
     */
    @Data
    public static class ComparativeMetrics {
        private MetricStat temperature;
        private MetricStat waveHeight;
        private MetricStat windSpeed;
        private MetricStat stabilityIndex;
    }

    @Data
    public static class MetricStat {
        /** 最小值 */
        private BigDecimal min;
        /** 最大值 */
        private BigDecimal max;
        /** 平均值 */
        private BigDecimal mean;
        /** 极差（max-min） */
        private BigDecimal range;
        /** 标准差 */
        private BigDecimal stddev;
        /** 数值最小的站点 ID */
        private Long minStationId;
        /** 数值最大的站点 ID */
        private Long maxStationId;
    }

    /**
     * 空间异常：某站某指标偏离群体 > 2σ
     */
    @Data
    public static class SpatialAnomaly {
        private Long dataSourceId;
        private String sourceName;
        /** 指标名（如 WAVE_HEIGHT） */
        private String metric;
        /** 当前站点的值 */
        private BigDecimal value;
        /** 群体均值 */
        private BigDecimal groupMean;
        /** 偏离标准差倍数（z-score） */
        private BigDecimal zScore;
        /** HIGH（显著偏高）/LOW（显著偏低） */
        private String severity;
        /** 描述（人类可读） */
        private String description;
    }
}

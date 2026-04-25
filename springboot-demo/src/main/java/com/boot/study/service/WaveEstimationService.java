package com.boot.study.service;

import lombok.Data;

import java.util.List;

/**
 * 波浪参数估算服务
 * <p>
 * 解决 NDBC 站点"只有 68% 有波浪数据"问题的核心服务。
 * 通过三级回退链确保任意站点都能得到可用波面参数：
 * <ol>
 *   <li>实测值（WVHT/DPD/MWD）</li>
 *   <li>Pierson-Moskowitz 公式从风速估算（Pierson & Moskowitz 1964）</li>
 *   <li>IDW 空间插值（Shepard 1968）</li>
 *   <li>气候默认值</li>
 * </ol>
 *
 * 参考文献：
 * - Pierson W J, Moskowitz L. "A proposed spectral form for fully developed wind seas
 *   based on the similarity theory of S. A. Kitaigorodskii." J. Geophys. Res., 69(24), 1964.
 * - Shepard D. "A two-dimensional interpolation function for irregularly-spaced data."
 *   Proceedings of the 1968 ACM National Conference, 1968.
 */
public interface WaveEstimationService {

    /**
     * 估算结果 VO（含置信度）
     */
    @Data
    class WaveEstimate {
        /** 有效波高 Hs (m) */
        private double waveHeight;
        /** 主周期 Tp (s) */
        private double wavePeriod;
        /** 主波向 MWD (°)，从北顺时针 */
        private Double waveDirection;
        /** 置信度：REAL / ESTIMATED / INTERPOLATED / DEFAULT */
        private String confidence;
        /** 估算方法描述 */
        private String method;
    }

    /**
     * 邻近站点样本（用于 IDW 插值）
     */
    @Data
    class NeighborSample {
        private double longitude;
        private double latitude;
        private double waveHeight;
        /** 可空，nullable */
        private Double wavePeriod;
        private Double waveDirection;
    }

    /**
     * 从风速估算波浪参数（Pierson-Moskowitz 1964 充分发展海浪谱）
     * <p>
     * 公式：<br>
     * H_s = 0.21 · U² / g（U 为 10m 风速，g=9.81）<br>
     * T_p = 7.14 · U / g（由 f_p 换算）
     *
     * @param windSpeed10m 10 米高度风速 (m/s)
     * @return 估算结果，confidence = ESTIMATED
     */
    WaveEstimate estimateFromWind(double windSpeed10m);

    /**
     * IDW (Inverse Distance Weighted) 空间插值（Shepard 1968）
     * <p>
     * 公式：V(x) = Σ(wᵢ·Vᵢ) / Σ(wᵢ)，其中 wᵢ = 1/d(x,xᵢ)^p（p=2）
     *
     * @param targetLon 目标经度
     * @param targetLat 目标纬度
     * @param neighbors 邻近站点样本列表
     * @return 插值结果，confidence = INTERPOLATED；若 neighbors 为空返回 null
     */
    WaveEstimate interpolateFromNeighbors(double targetLon, double targetLat, List<NeighborSample> neighbors);

    /**
     * 返回气候默认值（全球平均海况）
     * <ul>
     *   <li>Hs = 1.5 m</li>
     *   <li>T  = 7.0 s</li>
     *   <li>MWD = null</li>
     * </ul>
     *
     * @return 默认估算，confidence = DEFAULT
     */
    WaveEstimate defaultClimatology();

    /**
     * 完整回退链：输入所有可得信息，返回最可靠的估算。
     *
     * @param realWaveHeight 实测波高（可空）
     * @param realPeriod     实测周期（可空）
     * @param realDirection  实测波向（可空）
     * @param windSpeed10m   风速（可空，有值时启用 P-M 回退）
     * @param targetLon      目标经度（可空，有邻居时启用 IDW）
     * @param targetLat      目标纬度
     * @param neighbors      邻近站点（可空）
     * @return 估算结果（永不为 null）
     */
    WaveEstimate resolveWithFallback(
            Double realWaveHeight,
            Double realPeriod,
            Double realDirection,
            Double windSpeed10m,
            Double targetLon,
            Double targetLat,
            List<NeighborSample> neighbors);
}

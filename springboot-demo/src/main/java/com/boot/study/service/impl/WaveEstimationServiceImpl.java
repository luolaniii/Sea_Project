package com.boot.study.service.impl;

import com.boot.study.service.WaveEstimationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 波浪估算服务实现
 *
 * @see WaveEstimationService
 */
@Service
public class WaveEstimationServiceImpl implements WaveEstimationService {

    /** 重力加速度 (m/s²) */
    private static final double G = 9.81;

    /** IDW 幂参数（Shepard 1968 推荐 p=2） */
    private static final double IDW_POWER = 2.0;

    /** 最大搜索半径：超过此距离的邻居视为无效（km） */
    private static final double MAX_NEIGHBOR_KM = 500.0;

    @Override
    public WaveEstimate estimateFromWind(double u10) {
        double u = Math.max(0, u10);
        WaveEstimate e = new WaveEstimate();
        e.setWaveHeight(clamp(0.21 * u * u / G, 0, 20));
        e.setWavePeriod(clamp(7.14 * u / G, 2, 25));
        e.setWaveDirection(null);
        e.setConfidence("ESTIMATED");
        e.setMethod(String.format("Pierson-Moskowitz (1964): Hs=0.21U²/g, Tp=7.14U/g, U=%.2f m/s", u));
        return e;
    }

    @Override
    public WaveEstimate interpolateFromNeighbors(double targetLon, double targetLat, List<NeighborSample> neighbors) {
        if (neighbors == null || neighbors.isEmpty()) return null;

        double sumW = 0, sumHs = 0, sumT = 0, sumCos = 0, sumSin = 0;
        int usedT = 0, usedDir = 0, usedHs = 0;
        double minDist = Double.MAX_VALUE;

        for (NeighborSample n : neighbors) {
            double d = haversineKm(targetLon, targetLat, n.getLongitude(), n.getLatitude());
            if (d > MAX_NEIGHBOR_KM) continue;
            // 零距离保护：重合点直接取其值
            if (d < 1e-6) {
                WaveEstimate e = new WaveEstimate();
                e.setWaveHeight(n.getWaveHeight());
                e.setWavePeriod(n.getWavePeriod() == null ? 0 : n.getWavePeriod());
                e.setWaveDirection(n.getWaveDirection());
                e.setConfidence("INTERPOLATED");
                e.setMethod("IDW: coincident neighbor");
                return e;
            }
            double w = 1.0 / Math.pow(d, IDW_POWER);
            sumW += w;
            sumHs += w * n.getWaveHeight();
            usedHs++;
            if (n.getWavePeriod() != null) {
                sumT += w * n.getWavePeriod();
                usedT++;
            }
            if (n.getWaveDirection() != null) {
                double rad = Math.toRadians(n.getWaveDirection());
                sumCos += w * Math.cos(rad);
                sumSin += w * Math.sin(rad);
                usedDir++;
            }
            if (d < minDist) minDist = d;
        }
        if (sumW <= 0 || usedHs == 0) return null;

        WaveEstimate e = new WaveEstimate();
        e.setWaveHeight(sumHs / sumW);
        e.setWavePeriod(usedT > 0 ? sumT / sumW : 6);
        if (usedDir > 0) {
            double dirDeg = Math.toDegrees(Math.atan2(sumSin / sumW, sumCos / sumW));
            if (dirDeg < 0) dirDeg += 360;
            e.setWaveDirection(dirDeg);
        }
        e.setConfidence("INTERPOLATED");
        e.setMethod(String.format("IDW (p=%.0f, Shepard 1968): %d neighbors within %.1f km, min dist=%.1f km",
                IDW_POWER, usedHs, MAX_NEIGHBOR_KM, minDist));
        return e;
    }

    @Override
    public WaveEstimate defaultClimatology() {
        WaveEstimate e = new WaveEstimate();
        e.setWaveHeight(1.5);
        e.setWavePeriod(7.0);
        e.setWaveDirection(null);
        e.setConfidence("DEFAULT");
        e.setMethod("Global climatological average");
        return e;
    }

    @Override
    public WaveEstimate resolveWithFallback(
            Double realWaveHeight, Double realPeriod, Double realDirection,
            Double windSpeed10m, Double targetLon, Double targetLat,
            List<NeighborSample> neighbors) {

        // 第一级：实测优先
        if (isValid(realWaveHeight) && realWaveHeight > 0) {
            WaveEstimate e = new WaveEstimate();
            e.setWaveHeight(realWaveHeight);
            e.setWavePeriod(isValid(realPeriod) && realPeriod > 0 ? realPeriod : 6);
            e.setWaveDirection(isValid(realDirection) ? realDirection : null);
            e.setConfidence("REAL");
            e.setMethod("Direct observation (WVHT/DPD/MWD)");
            return e;
        }

        // 第二级：P-M 公式从风速估算
        if (isValid(windSpeed10m) && windSpeed10m > 0) {
            return estimateFromWind(windSpeed10m);
        }

        // 第三级：IDW 空间插值
        if (isValid(targetLon) && isValid(targetLat) && neighbors != null && !neighbors.isEmpty()) {
            WaveEstimate idw = interpolateFromNeighbors(targetLon, targetLat, neighbors);
            if (idw != null) return idw;
        }

        // 第四级：气候默认
        return defaultClimatology();
    }

    // ============ helpers ============

    private static boolean isValid(Double v) {
        return v != null && !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Haversine 距离（两地球面点距离，公里）
     */
    private static double haversineKm(double lon1, double lat1, double lon2, double lat2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

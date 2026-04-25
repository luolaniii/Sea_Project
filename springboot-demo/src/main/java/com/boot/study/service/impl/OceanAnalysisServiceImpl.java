package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boot.study.api.req.user.OceanAnalysisReq;
import com.boot.study.api.req.user.OceanComparisonReq;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.ObservationData;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.entity.OceanAbnormalResultVO;
import com.boot.study.entity.OceanComfortResultVO;
import com.boot.study.entity.OceanComparisonResultVO;
import com.boot.study.entity.OceanCompositeResultVO;
import com.boot.study.entity.OceanStabilityResultVO;
import com.boot.study.entity.OceanTrendResultVO;
import com.boot.study.exception.ServiceException;
import com.boot.study.service.DataSourceService;
import com.boot.study.service.ObservationDataService;
import com.boot.study.service.ObservationDataTypeService;
import com.boot.study.service.OceanAnalysisService;
import com.boot.study.utils.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OceanAnalysisServiceImpl implements OceanAnalysisService {

    private static final String TEMP = "TEMP";
    private static final String SST = "SST";
    private static final String SAL = "SAL";
    private static final String SEA_LEVEL = "SEA_LEVEL";
    private static final String WAVE_HEIGHT = "WAVE_HEIGHT";
    private static final String WIND_SPEED = "WIND_SPEED";

    private final ObservationDataService observationDataService;
    private final ObservationDataTypeService observationDataTypeService;
    private final DataSourceService dataSourceService;
    private final Map<String, CacheEntry<AnalysisContext>> contextCache = new ConcurrentHashMap<>();
    private volatile CacheEntry<Map<String, Long>> requiredTypeMapCache;
    private static final long CONTEXT_CACHE_TTL_MS = 30_000L;
    private static final long TYPE_CACHE_TTL_MS = 300_000L;

    @Override
    public OceanAbnormalResultVO abnormal(OceanAnalysisReq req) {
        AnalysisContext ctx = loadContextWithCache(req);
        return calcAbnormal(ctx);
    }

    @Override
    public OceanStabilityResultVO stability(OceanAnalysisReq req) {
        AnalysisContext ctx = loadContextWithCache(req);
        return calcStability(ctx);
    }

    @Override
    public OceanComfortResultVO comfort(OceanAnalysisReq req) {
        AnalysisContext ctx = loadContextWithCache(req);
        OceanStabilityResultVO stability = calcStability(ctx);
        return calcComfort(ctx, stability);
    }

    @Override
    public OceanTrendResultVO trend(OceanAnalysisReq req) {
        AnalysisContext ctx = loadContextWithCache(req);
        return calcTrend(ctx);
    }

    @Override
    public OceanCompositeResultVO composite(OceanAnalysisReq req) {
        AnalysisContext ctx = loadContextWithCache(req);
        OceanCompositeResultVO vo = new OceanCompositeResultVO();
        vo.setCurrentTime(ctx.currentTime);
        vo.setTemperature(toBig(ctx.currentTemp));
        vo.setSalinity(toBig(ctx.currentSal));
        vo.setSeaLevel(toBig(ctx.currentTide));
        vo.setWaveHeight(toBig(ctx.currentWave));
        vo.setWindSpeed(toBig(ctx.currentWind));
        vo.setAbnormal(calcAbnormal(ctx));
        OceanStabilityResultVO stability = calcStability(ctx);
        vo.setStability(stability);
        vo.setComfort(calcComfort(ctx, stability));
        vo.setTrend(calcTrend(ctx));
        vo.setSeries(buildSeries(ctx));
        return vo;
    }

    private OceanAbnormalResultVO calcAbnormal(AnalysisContext ctx) {
        OceanAbnormalResultVO vo = new OceanAbnormalResultVO();
        List<String> reasons = new ArrayList<>();

        double temp = ctx.currentTemp;
        double tide = ctx.currentTide;
        double wave = ctx.currentWave;
        double wind = ctx.currentWind;

        ReferenceValue temp15 = ctx.temp15;
        ReferenceValue tide15 = ctx.tide15;
        ReferenceValue wave15 = ctx.wave15;
        ReferenceValue wind15 = ctx.wind15;

        double avgTide = ctx.avgTide;

        boolean tempStatic = available(temp) && (temp < 10 || temp > 25);
        boolean tideStatic = available(tide) && available(avgTide) && Math.abs(tide - avgTide) > 0.5;
        boolean waveStatic = available(wave) && wave >= 1.5;
        boolean windStatic = available(wind) && wind >= 5.1;

        boolean tempDyn = available(temp) && available(temp15.value) && Math.abs(temp - temp15.value) >= 2;
        boolean tideDyn = available(tide) && available(tide15.value) && Math.abs(tide - tide15.value) >= 0.3;
        boolean waveDyn = available(wave) && available(wave15.value) && Math.abs(wave - wave15.value) >= 0.5;
        boolean windDyn = available(wind) && available(wind15.value) && Math.abs(wind - wind15.value) >= 2;

        if (!available(wave)) {
            reasons.add("波高缺失，波高异常判定已降级");
        }
        if (!available(temp15.value) || !available(tide15.value) || !available(wave15.value) || !available(wind15.value)) {
            reasons.add("15分钟前参考值部分缺失，动态异常判定已部分降级");
        }

        vo.setLevel1Alarm(tempStatic || tideStatic || tempDyn || tideDyn);
        vo.setLevel2Alarm(waveStatic || windStatic || waveDyn || windDyn);
        vo.setTempStaticAbnormal(tempStatic);
        vo.setTideStaticAbnormal(tideStatic);
        vo.setWaveStaticAbnormal(waveStatic);
        vo.setWindStaticAbnormal(windStatic);
        vo.setTempDynamicAbnormal(tempDyn);
        vo.setTideDynamicAbnormal(tideDyn);
        vo.setWaveDynamicAbnormal(waveDyn);
        vo.setWindDynamicAbnormal(windDyn);
        vo.setRef15mStatus(worstStatus(temp15.status, tide15.status, wave15.status, wind15.status));
        vo.setConfidence(toConfidence(vo.getRef15mStatus()));
        vo.setDegradeReason(reasons);
        return vo;
    }

    private OceanStabilityResultVO calcStability(AnalysisContext ctx) {
        OceanStabilityResultVO vo = new OceanStabilityResultVO();
        List<String> reasons = new ArrayList<>();

        double temp = ctx.currentTemp;
        double tide = ctx.currentTide;
        double wind = ctx.currentWind;
        ReferenceValue temp15 = ctx.temp15;
        double avgTide = ctx.avgTide;
        double avgWind = ctx.avgWind;
        List<Double> wave3 = ctx.waveLatest3;

        Double tempRate = available(temp) && available(temp15.value) ? Math.abs(temp - temp15.value) / 20.0 : null;
        Double tideRate = available(tide) && available(avgTide) ? Math.abs(tide - avgTide) / safeDivisor(avgTide) : null;
        Double waveRate = wave3.size() >= 3 ? variance(wave3) / 0.5 : null;
        Double windRate = available(wind) && available(avgWind) ? Math.abs(wind - avgWind) / safeDivisor(avgWind) : null;

        if (waveRate == null) {
            reasons.add("波高数据不足3个点，稳定性波高项已降级");
        }
        if (tempRate == null || tideRate == null || windRate == null) {
            reasons.add("部分关键项缺失，稳定性按有效项重归一化");
        }

        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("temp", 30.0);
        weights.put("tide", 30.0);
        weights.put("wave", 20.0);
        weights.put("wind", 20.0);
        Map<String, Double> rates = new HashMap<>();
        rates.put("temp", tempRate);
        rates.put("tide", tideRate);
        rates.put("wave", waveRate);
        rates.put("wind", windRate);

        double validWeight = weights.entrySet().stream()
                .filter(e -> rates.get(e.getKey()) != null)
                .mapToDouble(Map.Entry::getValue)
                .sum();
        if (validWeight <= 0) {
            throw new ServiceException(400, "稳定性计算失败：有效数据不足");
        }

        double penalty = 0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            Double rate = rates.get(entry.getKey());
            if (rate != null) {
                penalty += rate * (entry.getValue() * 100.0 / validWeight);
            }
        }
        double index = clamp(100 - penalty, 0, 100);
        vo.setStabilityIndex(toBig(index));
        vo.setLevel(stabilityLevel(index));
        vo.setTempRate(toBigOrNull(tempRate));
        vo.setTideRate(toBigOrNull(tideRate));
        vo.setWaveRate(toBigOrNull(waveRate));
        vo.setWindRate(toBigOrNull(windRate));
        vo.setConfidence(reasons.isEmpty() ? "HIGH" : "MEDIUM");
        vo.setDegradeReason(reasons);
        return vo;
    }

    private OceanComfortResultVO calcComfort(AnalysisContext ctx, OceanStabilityResultVO stability) {
        OceanComfortResultVO vo = new OceanComfortResultVO();
        List<String> reasons = new ArrayList<>();

        double temp = ctx.currentTemp;
        double tide = ctx.currentTide;
        double wind = ctx.currentWind;
        double avgTide = ctx.avgTide;
        double stabilityIndex = stability.getStabilityIndex() == null ? Double.NaN : stability.getStabilityIndex().doubleValue();

        if (!available(stabilityIndex)) {
            throw new ServiceException(400, "舒适度计算失败：稳定性缺失");
        }

        double tempScore = available(temp) ? ((temp >= 18 && temp <= 24) ? 100 : Math.max(0, 100 - 5 * Math.abs(temp - 21))) : 0;
        double tideScore = (available(tide) && available(avgTide)) ? Math.max(0, 100 - 100 * (Math.abs(tide - avgTide) / 0.5)) : 0;
        double windScore;
        if (!available(wind)) {
            windScore = 0;
        } else if (wind <= 3) {
            windScore = 100;
        } else if (wind <= 5.1) {
            windScore = 100 - 20 * (wind - 3);
        } else {
            windScore = 0;
        }

        if (!available(temp) || !available(tide) || !available(wind)) {
            reasons.add("舒适度部分输入缺失，结果可信度降低");
        }

        double total = tempScore * 0.4 + stabilityIndex * 0.3 + tideScore * 0.2 + windScore * 0.1;
        total = round2(total);
        vo.setScore(toBig(total));
        vo.setLevel(comfortLevel(total));
        vo.setSuggestion(comfortSuggestion(total));
        vo.setConfidence(reasons.isEmpty() ? "HIGH" : "MEDIUM");
        vo.setDegradeReason(reasons);
        return vo;
    }

    private OceanTrendResultVO calcTrend(AnalysisContext ctx) {
        OceanTrendResultVO vo = new OceanTrendResultVO();
        List<String> reasons = new ArrayList<>();

        List<Double> tempData = ctx.tempData;
        List<Double> tideData = ctx.tideData;

        if (tempData.size() < 2) {
            reasons.add("温度历史样本不足，趋势结果可能不稳定");
        }
        if (tideData.size() < 2) {
            reasons.add("潮位历史样本不足，趋势结果可能不稳定");
        }

        vo.setTemperaturePredict(toBigOrNull(sma(tempData, 12)));
        vo.setSeaLevelPredict(toBigOrNull(sma(tideData, 6)));
        vo.setTemperatureTrend(calcTrendDirection(tempData));
        vo.setSeaLevelTrend(calcTrendDirection(tideData));
        vo.setConfidence(reasons.isEmpty() ? "HIGH" : "MEDIUM");
        vo.setDegradeReason(reasons);
        return vo;
    }

    private List<OceanCompositeResultVO.SeriesPointVO> buildSeries(AnalysisContext ctx) {
        Map<LocalDateTime, OceanCompositeResultVO.SeriesPointVO> map = new TreeMap<>();
        fillSeries(map, ctx.tempSeries, v -> v::setTemperature);
        fillSeries(map, ctx.salSeries, v -> v::setSalinity);
        fillSeries(map, ctx.tideSeries, v -> v::setSeaLevel);
        fillSeries(map, ctx.waveSeries, v -> v::setWaveHeight);
        fillSeries(map, ctx.windSeries, v -> v::setWindSpeed);
        return new ArrayList<>(map.values());
    }

    private void fillSeries(Map<LocalDateTime, OceanCompositeResultVO.SeriesPointVO> map,
                            List<SeriesValue> series,
                            Function<OceanCompositeResultVO.SeriesPointVO, java.util.function.Consumer<BigDecimal>> setterProvider) {
        for (SeriesValue s : series) {
            OceanCompositeResultVO.SeriesPointVO point = map.computeIfAbsent(s.time, t -> {
                OceanCompositeResultVO.SeriesPointVO p = new OceanCompositeResultVO.SeriesPointVO();
                p.setTime(t);
                return p;
            });
            setterProvider.apply(point).accept(toBig(s.value));
        }
    }

    private AnalysisContext loadContextWithCache(OceanAnalysisReq req) {
        TimeRange range = resolveTimeRange(req);
        String key = buildContextCacheKey(req.getDataSourceId(), range.start, range.end);
        long now = System.currentTimeMillis();
        CacheEntry<AnalysisContext> cached = contextCache.get(key);
        if (cached != null && cached.expireAtMs > now) {
            return cached.value;
        }
        AnalysisContext fresh = loadContext(req, range.start, range.end);
        contextCache.put(key, new CacheEntry<>(fresh, now + CONTEXT_CACHE_TTL_MS));
        if (contextCache.size() > 300) {
            cleanupExpiredContextCache(now);
        }
        return fresh;
    }

    private void cleanupExpiredContextCache(long nowMs) {
        contextCache.entrySet().removeIf(e -> e.getValue().expireAtMs <= nowMs);
    }

    private TimeRange resolveTimeRange(OceanAnalysisReq req) {
        if (req.getDataSourceId() == null) {
            throw new ServiceException(400, "dataSourceId 不能为空");
        }
        LocalDateTime end = DateTimeUtil.parseToLocalDateTime(req.getEndTime());
        if (end == null) {
            end = LocalDateTime.now();
        }
        LocalDateTime start = DateTimeUtil.parseToLocalDateTime(req.getStartTime());
        if (start == null) {
            int hours = req.getHistoryHours() == null || req.getHistoryHours() <= 0 ? 24 : req.getHistoryHours();
            start = end.minusHours(hours);
        }
        if (start.isAfter(end)) {
            LocalDateTime tmp = start;
            start = end;
            end = tmp;
        }
        return new TimeRange(start, end);
    }

    private String buildContextCacheKey(Long dataSourceId, LocalDateTime start, LocalDateTime end) {
        return dataSourceId + "|" + start + "|" + end;
    }

    private AnalysisContext loadContext(OceanAnalysisReq req, LocalDateTime start, LocalDateTime end) {
        Set<String> typeCodes = new LinkedHashSet<>(Arrays.asList(TEMP, SST, SAL, SEA_LEVEL, WAVE_HEIGHT, WIND_SPEED));
        Map<String, Long> typeMap = getRequiredTypeMap(typeCodes);
        List<Long> typeIds = typeMap.values().stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(typeIds)) {
            throw new ServiceException(400, "未找到分析所需数据类型");
        }

        List<ObservationData> rows = observationDataService.list(Wrappers.<ObservationData>lambdaQuery()
                .eq(ObservationData::getDataSourceId, req.getDataSourceId())
                .in(ObservationData::getDataTypeId, typeIds)
                .ge(ObservationData::getObservationTime, start)
                .le(ObservationData::getObservationTime, end)
                .ne(ObservationData::getQualityFlag, "BAD")
                .orderByAsc(ObservationData::getObservationTime));

        Map<Long, List<ObservationData>> byType = rows.stream().collect(Collectors.groupingBy(ObservationData::getDataTypeId));

        AnalysisContext ctx = new AnalysisContext();
        ctx.currentTime = end;
        ctx.tempSeries = pickSeries(typeMap, byType, TEMP, SST);
        ctx.salSeries = pickSeries(typeMap, byType, SAL);
        ctx.tideSeries = pickSeries(typeMap, byType, SEA_LEVEL);
        ctx.waveSeries = pickSeries(typeMap, byType, WAVE_HEIGHT);
        ctx.windSeries = pickSeries(typeMap, byType, WIND_SPEED);
        initDerivedFields(ctx);
        return ctx;
    }

    private Map<String, Long> getRequiredTypeMap(Set<String> requiredCodes) {
        long now = System.currentTimeMillis();
        CacheEntry<Map<String, Long>> cached = requiredTypeMapCache;
        if (cached == null || cached.expireAtMs <= now) {
            List<ObservationDataType> types = observationDataTypeService.list(
                    Wrappers.<ObservationDataType>lambdaQuery().in(ObservationDataType::getTypeCode, requiredCodes)
            );
            Map<String, Long> data = types.stream()
                    .collect(Collectors.toMap(ObservationDataType::getTypeCode, ObservationDataType::getId, (a, b) -> a));
            requiredTypeMapCache = new CacheEntry<>(data, now + TYPE_CACHE_TTL_MS);
            cached = requiredTypeMapCache;
        }
        return cached.value;
    }

    private void initDerivedFields(AnalysisContext ctx) {
        ctx.currentTemp = getCurrentValue(ctx.tempSeries, ctx.currentTime);
        ctx.currentSal = getCurrentValue(ctx.salSeries, ctx.currentTime);
        ctx.currentTide = getCurrentValue(ctx.tideSeries, ctx.currentTime);
        ctx.currentWave = getCurrentValue(ctx.waveSeries, ctx.currentTime);
        ctx.currentWind = getCurrentValue(ctx.windSeries, ctx.currentTime);
        ctx.avgTide = average(ctx.tideSeries);
        ctx.avgWind = average(ctx.windSeries);
        ctx.temp15 = resolve15m(ctx.tempSeries, ctx.currentTime);
        ctx.tide15 = resolve15m(ctx.tideSeries, ctx.currentTime);
        ctx.wave15 = resolve15m(ctx.waveSeries, ctx.currentTime);
        ctx.wind15 = resolve15m(ctx.windSeries, ctx.currentTime);
        ctx.waveLatest3 = latestValues(ctx.waveSeries, 3);
        ctx.tempData = ctx.tempSeries.stream().map(s -> s.value).filter(Objects::nonNull).collect(Collectors.toList());
        ctx.tideData = ctx.tideSeries.stream().map(s -> s.value).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<SeriesValue> pickSeries(Map<String, Long> typeMap, Map<Long, List<ObservationData>> byType, String... candidates) {
        for (String code : candidates) {
            Long id = typeMap.get(code);
            if (id == null) {
                continue;
            }
            List<ObservationData> list = byType.getOrDefault(id, Collections.emptyList());
            if (!list.isEmpty()) {
                return list.stream()
                        .map(r -> new SeriesValue(r.getObservationTime(), r.getDataValue() == null ? null : r.getDataValue().doubleValue()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    private double getCurrentValue(List<SeriesValue> series, LocalDateTime now) {
        return series.stream()
                .filter(s -> !s.time.isAfter(now) && s.value != null)
                .reduce((a, b) -> b)
                .map(s -> s.value)
                .orElse(Double.NaN);
    }

    private ReferenceValue resolve15m(List<SeriesValue> series, LocalDateTime now) {
        if (series.isEmpty()) {
            return new ReferenceValue(Double.NaN, "MISSING");
        }
        LocalDateTime target = now.minusMinutes(15);
        SeriesValue exact = nearest(series, target, 2);
        if (exact != null) {
            return new ReferenceValue(exact.value, "EXACT");
        }
        SeriesValue near = nearest(series, target, 20);
        if (near != null) {
            return new ReferenceValue(near.value, "NEAR");
        }
        SeriesValue before = lastBefore(series, target);
        SeriesValue after = firstAfter(series, target);
        if (before != null && after != null) {
            double seconds = Duration.between(before.time, after.time).getSeconds();
            if (seconds > 0) {
                double ratio = (double) Duration.between(before.time, target).getSeconds() / seconds;
                double value = before.value + (after.value - before.value) * ratio;
                return new ReferenceValue(value, "INTERPOLATED");
            }
        }
        SeriesValue latest = lastBefore(series, now);
        if (latest != null && Duration.between(latest.time, now).toMinutes() <= 90) {
            return new ReferenceValue(latest.value, "CARRY_FORWARD");
        }
        return new ReferenceValue(Double.NaN, "MISSING");
    }

    private SeriesValue nearest(List<SeriesValue> series, LocalDateTime target, long toleranceMinutes) {
        return series.stream()
                .filter(s -> s.value != null)
                .min(Comparator.comparingLong(s -> Math.abs(Duration.between(s.time, target).toMinutes())))
                .filter(s -> Math.abs(Duration.between(s.time, target).toMinutes()) <= toleranceMinutes)
                .orElse(null);
    }

    private SeriesValue lastBefore(List<SeriesValue> series, LocalDateTime target) {
        return series.stream().filter(s -> s.value != null && !s.time.isAfter(target)).reduce((a, b) -> b).orElse(null);
    }

    private SeriesValue firstAfter(List<SeriesValue> series, LocalDateTime target) {
        return series.stream().filter(s -> s.value != null && !s.time.isBefore(target)).findFirst().orElse(null);
    }

    private double average(List<SeriesValue> series) {
        List<Double> vals = series.stream().map(s -> s.value).filter(Objects::nonNull).collect(Collectors.toList());
        if (vals.isEmpty()) {
            return Double.NaN;
        }
        return vals.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private List<Double> latestValues(List<SeriesValue> series, int count) {
        List<Double> vals = series.stream().map(s -> s.value).filter(Objects::nonNull).collect(Collectors.toList());
        int start = Math.max(0, vals.size() - count);
        return vals.subList(start, vals.size());
    }

    private double variance(List<Double> data) {
        double avg = data.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return data.stream().mapToDouble(v -> Math.pow(v - avg, 2)).sum() / data.size();
    }

    private Double sma(List<Double> data, int windowSize) {
        if (data.isEmpty()) {
            return null;
        }
        int start = Math.max(0, data.size() - windowSize);
        List<Double> recent = data.subList(start, data.size());
        return recent.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private String calcTrendDirection(List<Double> data) {
        if (data.size() < 2) {
            return "样本不足";
        }
        int n = data.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            int x = i + 1;
            double y = data.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return "平稳";
        }
        double k = (n * sumXY - sumX * sumY) / denominator;
        if (k > 0.01) {
            return "上升";
        }
        if (k < -0.01) {
            return "下降";
        }
        return "平稳";
    }

    private String stabilityLevel(double index) {
        if (index >= 90) return "Ⅰ级(极稳定)";
        if (index >= 80) return "Ⅱ级(稳定)";
        if (index >= 70) return "Ⅲ级(较稳定)";
        if (index >= 60) return "Ⅳ级(较波动)";
        return "Ⅴ级(极不稳定)";
    }

    private String comfortLevel(double score) {
        if (score >= 90) return "极度适宜";
        if (score >= 70) return "适宜";
        if (score >= 60) return "勉强适宜";
        return "不适宜";
    }

    private String comfortSuggestion(double score) {
        if (score >= 90) return "推荐游泳/潜水";
        if (score >= 70) return "可进行水上活动";
        if (score >= 60) return "谨慎活动";
        return "不建议水上活动";
    }

    private String worstStatus(String... statuses) {
        List<String> order = Arrays.asList("EXACT", "NEAR", "INTERPOLATED", "CARRY_FORWARD", "MISSING");
        return Arrays.stream(statuses).max(Comparator.comparingInt(order::indexOf)).orElse("MISSING");
    }

    private String toConfidence(String status) {
        if ("EXACT".equals(status) || "NEAR".equals(status)) return "HIGH";
        if ("INTERPOLATED".equals(status)) return "MEDIUM";
        return "LOW";
    }

    private boolean available(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double safeDivisor(double v) {
        return Math.max(Math.abs(v), 0.1);
    }

    private double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal toBig(double v) {
        if (!available(v)) {
            return null;
        }
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toBigOrNull(Double v) {
        if (v == null || !available(v)) {
            return null;
        }
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }

    private static class AnalysisContext {
        private LocalDateTime currentTime;
        private List<SeriesValue> tempSeries = new ArrayList<>();
        private List<SeriesValue> salSeries = new ArrayList<>();
        private List<SeriesValue> tideSeries = new ArrayList<>();
        private List<SeriesValue> waveSeries = new ArrayList<>();
        private List<SeriesValue> windSeries = new ArrayList<>();
        private double currentTemp;
        private double currentSal;
        private double currentTide;
        private double currentWave;
        private double currentWind;
        private double avgTide;
        private double avgWind;
        private ReferenceValue temp15;
        private ReferenceValue tide15;
        private ReferenceValue wave15;
        private ReferenceValue wind15;
        private List<Double> waveLatest3 = new ArrayList<>();
        private List<Double> tempData = new ArrayList<>();
        private List<Double> tideData = new ArrayList<>();
    }

    private static class SeriesValue {
        private final LocalDateTime time;
        private final Double value;

        private SeriesValue(LocalDateTime time, Double value) {
            this.time = time;
            this.value = value;
        }
    }

    private static class ReferenceValue {
        private final Double value;
        private final String status;

        private ReferenceValue(Double value, String status) {
            this.value = value;
            this.status = status;
        }
    }

    private static class TimeRange {
        private final LocalDateTime start;
        private final LocalDateTime end;

        private TimeRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class CacheEntry<T> {
        private final T value;
        private final long expireAtMs;

        private CacheEntry(T value, long expireAtMs) {
            this.value = value;
            this.expireAtMs = expireAtMs;
        }
    }

    // ======================================================================
    // Module I：多站点对比分析与空间异常检测
    // ======================================================================

    @Override
    public OceanComparisonResultVO compare(OceanComparisonReq req) {
        if (req.getDataSourceIds() == null || req.getDataSourceIds().size() < 2) {
            throw new ServiceException(400, "对比分析至少需要 2 个站点");
        }
        OceanComparisonResultVO result = new OceanComparisonResultVO();
        List<String> degradeReasons = new ArrayList<>();
        List<OceanComparisonResultVO.StationSummary> summaries = new ArrayList<>();

        // 逐站点加载上下文（复用现有 loadContextWithCache）
        for (Long dsId : req.getDataSourceIds()) {
            OceanAnalysisReq singleReq = new OceanAnalysisReq();
            singleReq.setDataSourceId(dsId);
            singleReq.setStartTime(req.getStartTime());
            singleReq.setEndTime(req.getEndTime());
            singleReq.setHistoryHours(req.getHistoryHours());
            try {
                AnalysisContext ctx = loadContextWithCache(singleReq);
                OceanComparisonResultVO.StationSummary s = new OceanComparisonResultVO.StationSummary();
                s.setDataSourceId(dsId);
                DataSource ds = dataSourceService.getById(dsId);
                if (ds != null) {
                    s.setSourceName(ds.getSourceName());
                    s.setStationId(ds.getStationId());
                    s.setLongitude(ds.getLongitude());
                    s.setLatitude(ds.getLatitude());
                }
                s.setTemperature(toBig(ctx.currentTemp));
                s.setSalinity(toBig(ctx.currentSal));
                s.setSeaLevel(toBig(ctx.currentTide));
                s.setWaveHeight(toBig(ctx.currentWave));
                s.setWindSpeed(toBig(ctx.currentWind));
                OceanStabilityResultVO stab = calcStability(ctx);
                s.setStabilityIndex(stab.getStabilityIndex());
                OceanAbnormalResultVO ab = calcAbnormal(ctx);
                s.setAbnormal(Boolean.TRUE.equals(ab.getLevel1Alarm()) || Boolean.TRUE.equals(ab.getLevel2Alarm()));
                summaries.add(s);
            } catch (Exception e) {
                degradeReasons.add("站点 " + dsId + " 数据加载失败：" + e.getMessage());
            }
        }
        result.setStations(summaries);

        if (summaries.size() < 2) {
            result.setConfidence("LOW");
            degradeReasons.add("有效站点不足 2 个，对比分析降级");
            result.setDegradeReason(degradeReasons);
            return result;
        }

        // 计算群体指标
        OceanComparisonResultVO.ComparativeMetrics cm = new OceanComparisonResultVO.ComparativeMetrics();
        cm.setTemperature(statFrom(summaries, OceanComparisonResultVO.StationSummary::getTemperature));
        cm.setWaveHeight(statFrom(summaries, OceanComparisonResultVO.StationSummary::getWaveHeight));
        cm.setWindSpeed(statFrom(summaries, OceanComparisonResultVO.StationSummary::getWindSpeed));
        cm.setStabilityIndex(statFrom(summaries, OceanComparisonResultVO.StationSummary::getStabilityIndex));
        result.setMetrics(cm);

        // 空间异常检测（z-score > 2.0 认为显著偏离）
        List<OceanComparisonResultVO.SpatialAnomaly> anomalies = new ArrayList<>();
        anomalies.addAll(detectAnomaliesForMetric(summaries, "TEMPERATURE", OceanComparisonResultVO.StationSummary::getTemperature, cm.getTemperature()));
        anomalies.addAll(detectAnomaliesForMetric(summaries, "WAVE_HEIGHT", OceanComparisonResultVO.StationSummary::getWaveHeight, cm.getWaveHeight()));
        anomalies.addAll(detectAnomaliesForMetric(summaries, "WIND_SPEED", OceanComparisonResultVO.StationSummary::getWindSpeed, cm.getWindSpeed()));
        anomalies.addAll(detectAnomaliesForMetric(summaries, "STABILITY", OceanComparisonResultVO.StationSummary::getStabilityIndex, cm.getStabilityIndex()));
        result.setAnomalies(anomalies);

        result.setConfidence(degradeReasons.isEmpty() ? "HIGH" : "MEDIUM");
        result.setDegradeReason(degradeReasons);
        return result;
    }

    private OceanComparisonResultVO.MetricStat statFrom(
            List<OceanComparisonResultVO.StationSummary> stations,
            java.util.function.Function<OceanComparisonResultVO.StationSummary, BigDecimal> getter) {
        List<OceanComparisonResultVO.StationSummary> valid = stations.stream()
                .filter(s -> getter.apply(s) != null)
                .collect(Collectors.toList());
        if (valid.size() < 2) return null;
        double[] vals = new double[valid.size()];
        double sum = 0;
        double minV = Double.POSITIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;
        Long minId = null, maxId = null;
        for (int i = 0; i < valid.size(); i++) {
            OceanComparisonResultVO.StationSummary s = valid.get(i);
            double v = getter.apply(s).doubleValue();
            vals[i] = v;
            sum += v;
            if (v < minV) { minV = v; minId = s.getDataSourceId(); }
            if (v > maxV) { maxV = v; maxId = s.getDataSourceId(); }
        }
        double mean = sum / vals.length;
        double sq = 0;
        for (double v : vals) sq += (v - mean) * (v - mean);
        double std = Math.sqrt(sq / vals.length);

        OceanComparisonResultVO.MetricStat ms = new OceanComparisonResultVO.MetricStat();
        ms.setMin(toBig(minV));
        ms.setMax(toBig(maxV));
        ms.setMean(toBig(mean));
        ms.setRange(toBig(maxV - minV));
        ms.setStddev(toBig(std));
        ms.setMinStationId(minId);
        ms.setMaxStationId(maxId);
        return ms;
    }

    private List<OceanComparisonResultVO.SpatialAnomaly> detectAnomaliesForMetric(
            List<OceanComparisonResultVO.StationSummary> stations,
            String metricName,
            java.util.function.Function<OceanComparisonResultVO.StationSummary, BigDecimal> getter,
            OceanComparisonResultVO.MetricStat stat) {
        List<OceanComparisonResultVO.SpatialAnomaly> list = new ArrayList<>();
        if (stat == null || stat.getStddev() == null || stat.getStddev().doubleValue() < 1e-6) return list;
        double mean = stat.getMean().doubleValue();
        double std = stat.getStddev().doubleValue();
        for (OceanComparisonResultVO.StationSummary s : stations) {
            BigDecimal v = getter.apply(s);
            if (v == null) continue;
            double z = (v.doubleValue() - mean) / std;
            if (Math.abs(z) < 2.0) continue;
            OceanComparisonResultVO.SpatialAnomaly a = new OceanComparisonResultVO.SpatialAnomaly();
            a.setDataSourceId(s.getDataSourceId());
            a.setSourceName(s.getSourceName());
            a.setMetric(metricName);
            a.setValue(v);
            a.setGroupMean(stat.getMean());
            a.setZScore(toBig(z));
            a.setSeverity(z > 0 ? "HIGH" : "LOW");
            a.setDescription(String.format("%s 在 %s 指标上%s（z=%.2fσ）",
                    s.getSourceName() == null ? ("#" + s.getDataSourceId()) : s.getSourceName(),
                    metricName,
                    z > 0 ? "显著偏高" : "显著偏低",
                    z));
            list.add(a);
        }
        return list;
    }
}


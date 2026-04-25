package com.boot.study.service.impl;

import com.boot.study.entity.ObservationData;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.service.DataQualityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据质量检测服务实现
 *
 * @author study
 * @since 2024
 */
@Slf4j
@Service
public class DataQualityServiceImpl implements DataQualityService {

    /** 质量标志常量 */
    private static final String QUALITY_GOOD = "GOOD";
    private static final String QUALITY_QUESTIONABLE = "QUESTIONABLE";
    private static final String QUALITY_BAD = "BAD";

    @Override
    public String evaluateQuality(ObservationData data,
                                  ObservationDataType dataType,
                                  List<ObservationData> recentData) {
        if (data == null || data.getDataValue() == null) {
            return QUALITY_BAD;
        }

        if (dataType == null) {
            // 无法进行质量检测，默认GOOD
            return QUALITY_GOOD;
        }

        BigDecimal value = data.getDataValue();

        // 1. 范围检测
        String rangeResult = checkRange(value, dataType);
        if (QUALITY_BAD.equals(rangeResult)) {
            return QUALITY_BAD;
        }

        // 2. 变化率检测
        String rateResult = checkRateOfChange(data, dataType, recentData);
        if (QUALITY_QUESTIONABLE.equals(rateResult)) {
            return QUALITY_QUESTIONABLE;
        }

        // 3. 尖峰检测
        String spikeResult = checkSpike(value, dataType, recentData);
        if (QUALITY_QUESTIONABLE.equals(spikeResult)) {
            return QUALITY_QUESTIONABLE;
        }

        // 4. 持续性检测
        String persistenceResult = checkPersistence(value, dataType, recentData);
        if (QUALITY_QUESTIONABLE.equals(persistenceResult)) {
            return QUALITY_QUESTIONABLE;
        }

        return QUALITY_GOOD;
    }

    @Override
    public List<ObservationData> evaluateBatch(List<ObservationData> dataList,
                                               Map<Long, ObservationDataType> typeMap) {
        if (dataList == null || dataList.isEmpty()) {
            return dataList;
        }

        // 按数据类型和时间排序，便于计算变化率
        Map<Long, List<ObservationData>> groupedByType = dataList.stream()
                .filter(d -> d.getDataTypeId() != null)
                .collect(Collectors.groupingBy(ObservationData::getDataTypeId));

        for (Map.Entry<Long, List<ObservationData>> entry : groupedByType.entrySet()) {
            Long typeId = entry.getKey();
            List<ObservationData> typeDataList = entry.getValue();
            ObservationDataType dataType = typeMap.get(typeId);

            // 按时间排序
            typeDataList.sort(Comparator.comparing(ObservationData::getObservationTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));

            // 逐条评估质量
            for (int i = 0; i < typeDataList.size(); i++) {
                ObservationData data = typeDataList.get(i);
                // 获取该条数据之前的最近数据（最多取10条）
                List<ObservationData> recentData = typeDataList.subList(
                        Math.max(0, i - 10), i);

                String qualityFlag = evaluateQuality(data, dataType, recentData);
                data.setQualityFlag(qualityFlag);
            }
        }

        // 处理没有类型ID的数据
        dataList.stream()
                .filter(d -> d.getDataTypeId() == null && d.getQualityFlag() == null)
                .forEach(d -> d.setQualityFlag(QUALITY_GOOD));

        return dataList;
    }

    @Override
    public Map<String, Integer> getQualityStatistics(List<ObservationData> dataList) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("goodCount", 0);
        stats.put("questionableCount", 0);
        stats.put("badCount", 0);
        stats.put("totalCount", 0);

        if (dataList == null || dataList.isEmpty()) {
            return stats;
        }

        stats.put("totalCount", dataList.size());

        for (ObservationData data : dataList) {
            String flag = data.getQualityFlag();
            if (QUALITY_GOOD.equals(flag)) {
                stats.put("goodCount", stats.get("goodCount") + 1);
            } else if (QUALITY_QUESTIONABLE.equals(flag)) {
                stats.put("questionableCount", stats.get("questionableCount") + 1);
            } else if (QUALITY_BAD.equals(flag)) {
                stats.put("badCount", stats.get("badCount") + 1);
            }
        }

        return stats;
    }

    /**
     * 范围检测：检查值是否在有效范围内
     */
    private String checkRange(BigDecimal value, ObservationDataType dataType) {
        BigDecimal minValue = dataType.getMinValue();
        BigDecimal maxValue = dataType.getMaxValue();

        if (minValue != null && value.compareTo(minValue) < 0) {
            log.debug("范围检测失败: {} < {}", value, minValue);
            return QUALITY_BAD;
        }

        if (maxValue != null && value.compareTo(maxValue) > 0) {
            log.debug("范围检测失败: {} > {}", value, maxValue);
            return QUALITY_BAD;
        }

        return QUALITY_GOOD;
    }

    /**
     * 变化率检测：检查与前一条数据的变化率是否超过阈值
     */
    private String checkRateOfChange(ObservationData data,
                                     ObservationDataType dataType,
                                     List<ObservationData> recentData) {
        BigDecimal rateLimit = dataType.getRateOfChangeLimit();
        if (rateLimit == null || recentData == null || recentData.isEmpty()) {
            return QUALITY_GOOD;
        }

        // 获取最近一条有效数据
        ObservationData lastData = recentData.get(recentData.size() - 1);
        if (lastData.getDataValue() == null || lastData.getObservationTime() == null
                || data.getObservationTime() == null) {
            return QUALITY_GOOD;
        }

        // 计算时间差（小时）
        Duration duration = Duration.between(lastData.getObservationTime(), data.getObservationTime());
        double hours = duration.toMinutes() / 60.0;
        if (hours <= 0) {
            return QUALITY_GOOD;
        }

        // 计算变化率（每小时）
        BigDecimal valueDiff = data.getDataValue().subtract(lastData.getDataValue()).abs();
        BigDecimal ratePerHour = valueDiff.divide(BigDecimal.valueOf(hours), 6, RoundingMode.HALF_UP);

        if (ratePerHour.compareTo(rateLimit) > 0) {
            log.debug("变化率检测异常: 变化率 {} > 阈值 {}", ratePerHour, rateLimit);
            return QUALITY_QUESTIONABLE;
        }

        return QUALITY_GOOD;
    }

    /**
     * 尖峰检测：检查值与近期均值的偏差是否超过阈值倍数的标准差
     */
    private String checkSpike(BigDecimal value,
                              ObservationDataType dataType,
                              List<ObservationData> recentData) {
        BigDecimal spikeThreshold = dataType.getSpikeThreshold();
        if (spikeThreshold == null || recentData == null || recentData.size() < 3) {
            return QUALITY_GOOD;
        }

        // 计算近期数据的均值和标准差
        List<BigDecimal> values = recentData.stream()
                .map(ObservationData::getDataValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (values.size() < 3) {
            return QUALITY_GOOD;
        }

        BigDecimal mean = calculateMean(values);
        BigDecimal stdDev = calculateStdDev(values, mean);

        if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
            // 标准差为0，无法检测尖峰
            return QUALITY_GOOD;
        }

        // 计算偏差倍数
        BigDecimal deviation = value.subtract(mean).abs();
        BigDecimal deviationRatio = deviation.divide(stdDev, 6, RoundingMode.HALF_UP);

        if (deviationRatio.compareTo(spikeThreshold) > 0) {
            log.debug("尖峰检测异常: 偏差 {} > {} 倍标准差", deviationRatio, spikeThreshold);
            return QUALITY_QUESTIONABLE;
        }

        return QUALITY_GOOD;
    }

    /**
     * 持续性检测：检查是否存在连续相同的值（可能传感器故障）
     */
    private String checkPersistence(BigDecimal value,
                                    ObservationDataType dataType,
                                    List<ObservationData> recentData) {
        Integer persistenceLimit = dataType.getPersistenceLimit();
        if (persistenceLimit == null || recentData == null || recentData.isEmpty()) {
            return QUALITY_GOOD;
        }

        // 计算连续相同值的数量
        int sameCount = 0;
        for (int i = recentData.size() - 1; i >= 0; i--) {
            ObservationData d = recentData.get(i);
            if (d.getDataValue() != null && d.getDataValue().compareTo(value) == 0) {
                sameCount++;
            } else {
                break;
            }
        }

        // 加上当前值
        sameCount++;

        if (sameCount > persistenceLimit) {
            log.debug("持续性检测异常: 连续 {} 个相同值 > 阈值 {}", sameCount, persistenceLimit);
            return QUALITY_QUESTIONABLE;
        }

        return QUALITY_GOOD;
    }

    /**
     * 计算均值
     */
    private BigDecimal calculateMean(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算标准差
     */
    private BigDecimal calculateStdDev(List<BigDecimal> values, BigDecimal mean) {
        if (values == null || values.size() < 2) {
            return BigDecimal.ZERO;
        }
        BigDecimal sumSquaredDiff = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            BigDecimal diff = v.subtract(mean);
            sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSquaredDiff.divide(
                BigDecimal.valueOf(values.size() - 1), 10, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
}

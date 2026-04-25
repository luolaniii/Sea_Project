package com.boot.study.service;

import com.boot.study.entity.ObservationData;
import com.boot.study.entity.ObservationDataType;

import java.util.List;
import java.util.Map;

/**
 * 数据质量检测服务接口
 * <p>
 * 提供观测数据的质量评估功能，包括：
 * - 范围检测：检查数据是否在有效范围内
 * - 变化率检测：检查数据变化是否过快
 * - 尖峰检测：检测异常突变值
 * - 持续性检测：检测传感器故障导致的持续相同值
 *
 * @author study
 * @since 2024
 */
public interface DataQualityService {

    /**
     * 评估单条数据质量
     *
     * @param data       观测数据
     * @param dataType   数据类型（含质量规则）
     * @param recentData 最近历史数据（用于变化率和持续性检测）
     * @return 质量标志：GOOD / QUESTIONABLE / BAD
     */
    String evaluateQuality(ObservationData data,
                           ObservationDataType dataType,
                           List<ObservationData> recentData);

    /**
     * 批量质量检测
     * <p>
     * 对数据列表进行质量检测，并设置每条数据的qualityFlag字段
     *
     * @param dataList 观测数据列表
     * @param typeMap  数据类型ID到数据类型的映射
     * @return 设置了质量标志的数据列表
     */
    List<ObservationData> evaluateBatch(List<ObservationData> dataList,
                                        Map<Long, ObservationDataType> typeMap);

    /**
     * 获取质量检测统计信息
     *
     * @param dataList 已评估的数据列表
     * @return 统计信息：goodCount, questionableCount, badCount
     */
    Map<String, Integer> getQualityStatistics(List<ObservationData> dataList);
}

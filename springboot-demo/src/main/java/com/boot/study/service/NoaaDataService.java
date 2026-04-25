package com.boot.study.service;

import com.boot.study.entity.ObservationData;

import java.util.List;

/**
 * NOAA数据服务接口
 * <p>
 * 提供NOAA数据的批量保存、去重等功能
 *
 * @author study
 * @since 2024
 */
public interface NoaaDataService {

    /**
     * 批量保存观测数据（自动去重）
     * <p>
     * 基于apiDataId进行去重，如果数据已存在则跳过
     *
     * @param dataList 观测数据列表
     * @return 实际保存的数据条数
     */
    int saveBatchWithDeduplication(List<ObservationData> dataList);

    /**
     * 检查数据是否已存在（基于apiDataId）
     *
     * @param apiDataId API数据ID
     * @return 是否存在
     */
    boolean existsByApiDataId(String apiDataId);

    /**
     * 批量检查数据是否已存在
     *
     * @param apiDataIds API数据ID列表
     * @return 已存在的API数据ID集合
     */
    List<String> findExistingApiDataIds(List<String> apiDataIds);
}


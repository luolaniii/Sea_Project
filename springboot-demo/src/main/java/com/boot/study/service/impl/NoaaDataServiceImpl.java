package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boot.study.dao.ObservationDataMapper;
import com.boot.study.entity.ObservationData;
import com.boot.study.keygen.SnowflakeKeygen;
import com.boot.study.service.NoaaDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * NOAA数据服务实现
 * <p>
 * 提供NOAA数据的批量保存、去重等功能
 *
 * @author study
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoaaDataServiceImpl implements NoaaDataService {

    private final ObservationDataMapper observationDataMapper;
    @Value("${noaa.ingest.insert-batch-size:1000}")
    private int insertBatchSize;

    /**
     * 批量保存观测数据（自动去重）
     * <p>
     * 基于apiDataId进行去重，如果数据已存在则跳过
     *
     * @param dataList 观测数据列表
     * @return 实际保存的数据条数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveBatchWithDeduplication(List<ObservationData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return 0;
        }

        // 先进行同批次内存去重（保证同批次相同 apiDataId 只保留一条）
        Map<String, ObservationData> uniqueMap = new LinkedHashMap<>();
        for (ObservationData data : dataList) {
            String apiDataId = data.getApiDataId();
            if (apiDataId == null || apiDataId.trim().isEmpty()) {
                continue;
            }
            // LinkedHashMap 保留首条，避免同批重复进入后续流程
            uniqueMap.putIfAbsent(apiDataId, data);
        }

        List<ObservationData> uniqueBatch = new ArrayList<>(uniqueMap.values());
        List<String> apiDataIds = new ArrayList<>(uniqueMap.keySet());

        if (apiDataIds.isEmpty()) {
            log.warn("数据列表中没有有效的apiDataId，无法进行去重");
            return 0;
        }

        // 查询已存在的数据
        List<String> existingIds = findExistingApiDataIds(apiDataIds);
        Set<String> existingSet = existingIds.stream().collect(Collectors.toSet());

        // 过滤出需要保存的新数据（批内已去重，再做库内去重）
        List<ObservationData> newDataList = uniqueBatch.stream()
                .filter(data -> {
                    String apiDataId = data.getApiDataId();
                    return apiDataId != null && !apiDataId.trim().isEmpty() && !existingSet.contains(apiDataId);
                })
                .collect(Collectors.toList());

        if (newDataList.isEmpty()) {
            log.info("所有数据都已存在，无需保存");
            return 0;
        }

        // 批量保存新数据（真正的批量 INSERT）
        int savedCount = 0;
        int batchSize = Math.max(100, insertBatchSize);

        for (int i = 0; i < newDataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, newDataList.size());
            List<ObservationData> batch = newDataList.subList(i, end);

            // 为每条数据生成主键ID（使用 Snowflake 全局唯一ID）
            for (ObservationData data : batch) {
                if (data.getId() == null) {
                    data.setId(SnowflakeKeygen.getNextId());
                }
            }

            try {
                int affected = observationDataMapper.insertBatch(batch);
                savedCount += affected;
                log.debug("批量保存数据完成，本批数量: {}，受影响行数: {}", batch.size(), affected);
            } catch (Exception e) {
                log.error("批量保存数据异常，本批数量: {}", batch.size(), e);
                // 出现异常时，不中断整个导入流程，继续后续批次
            }
        }

        int duplicateInBatch = dataList.size() - uniqueBatch.size();
        int duplicateInDb = uniqueBatch.size() - newDataList.size();
        log.info("批量保存完成，总数据量: {}，批内重复: {}，库内重复: {}，尝试插入: {}，成功保存: {}",
                dataList.size(), duplicateInBatch, duplicateInDb, newDataList.size(), savedCount);
        return savedCount;
    }

    /**
     * 检查数据是否已存在（基于apiDataId）
     *
     * @param apiDataId API数据ID
     * @return 是否存在
     */
    @Override
    public boolean existsByApiDataId(String apiDataId) {
        if (apiDataId == null || apiDataId.trim().isEmpty()) {
            return false;
        }

        LambdaQueryWrapper<ObservationData> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObservationData::getApiDataId, apiDataId);
        wrapper.last("LIMIT 1");

        return observationDataMapper.selectCount(wrapper) > 0;
    }

    /**
     * 批量检查数据是否已存在
     *
     * @param apiDataIds API数据ID列表
     * @return 已存在的API数据ID集合
     */
    @Override
    public List<String> findExistingApiDataIds(List<String> apiDataIds) {
        if (apiDataIds == null || apiDataIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 去重
        List<String> distinctIds = apiDataIds.stream()
                .filter(id -> id != null && !id.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (distinctIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 分批查询（避免IN子句过长），并去重返回结果
        List<String> existingIds = new ArrayList<>();
        int batchSize = 1000;

        for (int i = 0; i < distinctIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, distinctIds.size());
            List<String> batch = distinctIds.subList(i, end);

            try {
                LambdaQueryWrapper<ObservationData> wrapper = Wrappers.lambdaQuery();
                wrapper.select(ObservationData::getApiDataId);
                wrapper.in(ObservationData::getApiDataId, batch);

                List<ObservationData> existing = observationDataMapper.selectList(wrapper);
                List<String> batchExisting = existing.stream()
                        .map(ObservationData::getApiDataId)
                        .filter(id -> id != null)
                        .collect(Collectors.toList());

                existingIds.addAll(batchExisting);
            } catch (Exception e) {
                log.error("批量查询已存在数据失败", e);
            }
        }

        return new ArrayList<>(new HashSet<>(existingIds));
    }
}


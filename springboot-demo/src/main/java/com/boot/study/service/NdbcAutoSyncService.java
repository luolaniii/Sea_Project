package com.boot.study.service;

import com.boot.study.entity.DataSource;

import java.util.List;
import java.util.Map;

/**
 * NDBC自动采集服务接口
 * <p>
 * 提供NOAA NDBC数据的HTTP自动采集功能
 *
 * @author study
 * @since 2024
 */
public interface NdbcAutoSyncService {

    /**
     * 同步所有启用自动同步的站点
     *
     * @return 同步结果汇总
     */
    Map<String, Object> syncAllEnabledStations();

    /**
     * 同步单个数据源的数据
     *
     * @param dataSource 数据源配置
     * @return 同步结果
     */
    Map<String, Object> syncStation(DataSource dataSource);

    /**
     * 从远程获取NDBC文件内容
     *
     * @param baseUrl   基础URL（如 https://www.ndbc.noaa.gov/data/realtime2）
     * @param stationId 站点ID（如 TIBC1）
     * @param suffix    文件后缀（如 txt、ocean）
     * @return 文件内容
     */
    String fetchNdbcFile(String baseUrl, String stationId, String suffix);

    /**
     * 预览远程NDBC数据（不入库）
     *
     * @param dataSourceId 数据源ID
     * @return 预览结果
     */
    Map<String, Object> previewRemoteData(Long dataSourceId);

    /**
     * 查询站点可用的文件类型
     *
     * @param stationId 站点ID
     * @return 可用的文件后缀列表
     */
    List<String> getAvailableFileSuffixes(String stationId);

    /**
     * 获取需要同步的数据源列表
     *
     * @return 需要同步的数据源列表
     */
    List<DataSource> findStationsToSync();
}

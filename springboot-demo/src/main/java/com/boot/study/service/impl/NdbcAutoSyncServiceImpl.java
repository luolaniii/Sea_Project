package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.ObservationData;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * NDBC自动采集服务实现
 *
 * @author study
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NdbcAutoSyncServiceImpl implements NdbcAutoSyncService {

    private final DataSourceService dataSourceService;
    private final ObservationDataTypeService observationDataTypeService;
    private final NoaaDataService noaaDataService;
    private final DataQualityService dataQualityService;
    private final RestTemplate restTemplate;
    private final Executor taskExecutor;

    /** 默认基础URL */
    private static final String DEFAULT_BASE_URL = "https://www.ndbc.noaa.gov/data/realtime2";

    /** 支持的文件后缀 */
    private static final List<String> SUPPORTED_SUFFIXES = Arrays.asList(
            "txt", "ocean", "cwind", "spec", "data_spec", "dart", "adcp",
            "rain", "srad", "supl", "tide", "wlevel", "pwind", "swden"
    );
    /** 变量映射缓存TTL（毫秒） */
    private static final long VARIABLE_TYPE_CACHE_TTL_MS = 5 * 60 * 1000L;
    /** 拉取重试次数 */
    private static final int FETCH_RETRY_TIMES = 2;
    /** 拉取重试间隔（毫秒） */
    private static final long FETCH_RETRY_BACKOFF_MS = 500L;
    /** 可用后缀探测并发数（受线程池限制） */
    private static final int AVAILABLE_SUFFIX_TIMEOUT_SECONDS = 6;

    private volatile CacheEntry<Map<String, ObservationDataType>> variableTypeMapCache;
    private final Map<String, Long> lastFetchFailedAt = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> syncAllEnabledStations() {
        Map<String, Object> result = new HashMap<>();
        List<DataSource> stations = findStationsToSync();

        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (DataSource station : stations) {
            try {
                Map<String, Object> stationResult = syncStation(station);
                details.add(stationResult);
                if ("success".equals(stationResult.get("status"))) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("stationId", station.getStationId());
                errorResult.put("status", "error");
                errorResult.put("message", e.getMessage());
                details.add(errorResult);
                log.error("同步站点 {} 失败", station.getStationId(), e);
            }
        }

        result.put("totalStations", stations.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("details", details);
        result.put("syncTime", LocalDateTime.now());

        return result;
    }

    @Override
    public Map<String, Object> syncStation(DataSource dataSource) {
        long startNs = System.nanoTime();
        Map<String, Object> result = new HashMap<>();
        result.put("dataSourceId", dataSource.getId());
        result.put("stationId", dataSource.getStationId());
        result.put("sourceName", dataSource.getSourceName());

        if (dataSource.getStationId() == null || dataSource.getStationId().trim().isEmpty()) {
            result.put("status", "error");
            result.put("message", "站点ID未配置");
            return result;
        }

        String baseUrl = dataSource.getApiUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        baseUrl = normalizeNdbcBaseUrl(baseUrl);

        // 获取要采集的文件后缀
        List<String> suffixes = parseSuffixes(dataSource.getFileSuffixes());
        if (suffixes.isEmpty()) {
            suffixes = Collections.singletonList("txt"); // 默认只采集标准气象文件
        }

        // 加载所有数据类型（用于变量名映射）
        Map<String, ObservationDataType> variableTypeMap = loadVariableTypeMap();
        Map<Long, ObservationDataType> typeIdMap = new HashMap<>();
        variableTypeMap.values().forEach(t -> typeIdMap.put(t.getId(), t));

        int totalParsed = 0;
        int totalSaved = 0;
        List<Map<String, Object>> fileResults = new ArrayList<>();

        for (String suffix : suffixes) {
            long fileStartNs = System.nanoTime();
            Map<String, Object> fileResult = new HashMap<>();
            fileResult.put("suffix", suffix);

            try {
                FetchResult fetch = fetchNdbcFileWithMeta(baseUrl, dataSource.getStationId(), suffix);
                fileResult.put("fetchCostMs", fetch.costMs);
                fileResult.put("fetchAttempts", fetch.attempts);
                if (!fetch.success) {
                    fileResult.put("status", "error");
                    fileResult.put("errorType", fetch.errorType);
                    fileResult.put("message", fetch.message);
                    fileResults.add(fileResult);
                    continue;
                }
                String content = fetch.content;
                if (content == null || content.trim().isEmpty()) {
                    fileResult.put("status", "empty");
                    fileResult.put("message", "文件为空或不存在");
                    fileResults.add(fileResult);
                    continue;
                }

                // 解析文件
                List<ObservationData> dataList = parseNdbcContent(
                        content,
                        dataSource.getId(),
                        dataSource.getLongitude(),
                        dataSource.getLatitude(),
                        variableTypeMap
                );

                if (dataList.isEmpty()) {
                    fileResult.put("status", "noData");
                    fileResult.put("message", "未解析到有效数据");
                    fileResults.add(fileResult);
                    continue;
                }

                totalParsed += dataList.size();

                // 质量检测
                dataList = dataQualityService.evaluateBatch(dataList, typeIdMap);
                Map<String, Integer> qualityStats = dataQualityService.getQualityStatistics(dataList);

                // 保存数据
                int savedCount = noaaDataService.saveBatchWithDeduplication(dataList);
                totalSaved += savedCount;

                fileResult.put("status", "success");
                fileResult.put("parsed", dataList.size());
                fileResult.put("saved", savedCount);
                fileResult.put("duplicate", dataList.size() - savedCount);
                fileResult.put("qualityStats", qualityStats);
                fileResult.put("costMs", (System.nanoTime() - fileStartNs) / 1_000_000);

            } catch (Exception e) {
                fileResult.put("status", "error");
                fileResult.put("message", e.getMessage());
                fileResult.put("costMs", (System.nanoTime() - fileStartNs) / 1_000_000);
                log.warn("采集文件 {}.{}.txt 失败: {}",
                        dataSource.getStationId(), suffix, e.getMessage());
            }

            fileResults.add(fileResult);
        }

        // 更新最后同步时间
        dataSource.setLastSyncTime(LocalDateTime.now());
        dataSourceService.updateById(dataSource);

        long successFiles = fileResults.stream().filter(m -> "success".equals(m.get("status"))).count();
        long errorFiles = fileResults.stream().filter(m -> "error".equals(m.get("status"))).count();
        long emptyOrNoDataFiles = fileResults.stream()
                .filter(m -> "empty".equals(m.get("status")) || "noData".equals(m.get("status"))).count();

        String finalStatus;
        if (successFiles == 0 && (errorFiles > 0 || emptyOrNoDataFiles > 0)) {
            finalStatus = "error";
        } else if (successFiles > 0 && (errorFiles > 0 || emptyOrNoDataFiles > 0)) {
            finalStatus = "partial";
        } else {
            finalStatus = "success";
        }

        result.put("status", finalStatus);
        result.put("totalParsed", totalParsed);
        result.put("totalSaved", totalSaved);
        result.put("totalDuplicate", Math.max(0, totalParsed - totalSaved));
        result.put("fileResults", fileResults);
        result.put("syncTime", LocalDateTime.now());
        long costMs = (System.nanoTime() - startNs) / 1_000_000;
        result.put("costMs", costMs);
        double seconds = Math.max(1d, costMs / 1000d);
        result.put("savePerSecond", Math.round((totalSaved / seconds) * 100.0) / 100.0);
        result.put("parsePerSecond", Math.round((totalParsed / seconds) * 100.0) / 100.0);
        result.put("duplicateRate", totalParsed <= 0 ? 0d
                : Math.round(((totalParsed - totalSaved) * 10000.0 / totalParsed)) / 100.0);

        return result;
    }

    @Override
    public String fetchNdbcFile(String baseUrl, String stationId, String suffix) {
        return fetchNdbcFileWithMeta(baseUrl, stationId, suffix).content;
    }

    private FetchResult fetchNdbcFileWithMeta(String baseUrl, String stationId, String suffix) {
        long startNs = System.nanoTime();
        String sid = stationId == null ? "" : stationId.trim().toUpperCase(Locale.ROOT);
        String suffixNorm = suffix == null ? "" : suffix.trim().replaceFirst("^\\.", "").toLowerCase(Locale.ROOT);
        String fileName = sid + "." + suffixNorm;
        String url = normalizeNdbcBaseUrl(baseUrl) + "/" + fileName;

        log.debug("获取NDBC文件: {}", url);
        String lastErrorType = null;
        String lastErrorMsg = null;

        for (int attempt = 0; attempt <= FETCH_RETRY_TIMES; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return FetchResult.success(response.getBody(), attempt + 1, elapsedMs(startNs));
                } else {
                    lastErrorType = "HTTP_" + response.getStatusCode().value();
                    lastErrorMsg = "HTTP状态码: " + response.getStatusCode().value();
                    log.warn("获取NDBC文件失败，状态码: {}，url: {}，attempt: {}", response.getStatusCode(), url, attempt + 1);
                }
            } catch (HttpStatusCodeException e) {
                lastErrorType = "HTTP_" + e.getStatusCode().value();
                lastErrorMsg = e.getMessage();
                log.warn("获取NDBC文件HTTP异常: {} - {}，attempt: {}", url, e.getStatusCode(), attempt + 1);
            } catch (ResourceAccessException e) {
                lastErrorType = "NETWORK_TIMEOUT";
                lastErrorMsg = e.getMessage();
                log.warn("获取NDBC文件网络异常: {} - {}，attempt: {}", url, e.getMessage(), attempt + 1);
            } catch (Exception e) {
                lastErrorType = "UNKNOWN";
                lastErrorMsg = e.getMessage();
                log.warn("获取NDBC文件异常: {} - {}，attempt: {}", url, e.getMessage(), attempt + 1);
                lastFetchFailedAt.put(url, System.currentTimeMillis());
            }
            if (attempt < FETCH_RETRY_TIMES) {
                try {
                    Thread.sleep(FETCH_RETRY_BACKOFF_MS * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return FetchResult.fail(
                lastErrorType == null ? "EMPTY_OR_NOT_FOUND" : lastErrorType,
                lastErrorMsg == null ? "文件为空或不存在" : lastErrorMsg,
                FETCH_RETRY_TIMES + 1,
                elapsedMs(startNs)
        );
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    @Override
    public Map<String, Object> previewRemoteData(Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();

        DataSource dataSource = dataSourceService.getById(dataSourceId);
        if (dataSource == null) {
            result.put("error", "数据源不存在");
            return result;
        }

        String baseUrl = dataSource.getApiUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        baseUrl = normalizeNdbcBaseUrl(baseUrl);

        String stationId = dataSource.getStationId();
        if (stationId == null || stationId.trim().isEmpty()) {
            result.put("error", "站点ID未配置");
            return result;
        }

        List<String> suffixes = parseSuffixes(dataSource.getFileSuffixes());
        if (suffixes.isEmpty()) {
            suffixes = Collections.singletonList("txt");
        }

        Map<String, ObservationDataType> variableTypeMap = loadVariableTypeMap();
        List<Map<String, Object>> previewList = new ArrayList<>();

        for (String suffix : suffixes) {
            Map<String, Object> preview = new HashMap<>();
            preview.put("suffix", suffix);

            FetchResult fetch = fetchNdbcFileWithMeta(baseUrl, stationId, suffix);
            preview.put("fetchCostMs", fetch.costMs);
            preview.put("fetchAttempts", fetch.attempts);
            if (fetch.success && fetch.content != null && !fetch.content.trim().isEmpty()) {
                // 只解析前10条数据用于预览
                List<ObservationData> dataList = parseNdbcContent(
                        fetch.content, dataSourceId,
                        dataSource.getLongitude(),
                        dataSource.getLatitude(),
                        variableTypeMap
                );

                preview.put("available", true);
                preview.put("totalRecords", dataList.size());
                preview.put("sampleData", dataList.stream().limit(5).collect(Collectors.toList()));
                preview.put("variables", extractVariables(fetch.content));
            } else {
                preview.put("available", false);
                if (!fetch.success) {
                    preview.put("errorType", fetch.errorType);
                    preview.put("message", fetch.message);
                }
            }

            previewList.add(preview);
        }

        result.put("stationId", stationId);
        result.put("files", previewList);

        return result;
    }

    @Override
    public List<String> getAvailableFileSuffixes(String stationId) {
        List<CompletableFuture<String>> tasks = SUPPORTED_SUFFIXES.stream()
                .map(suffix -> CompletableFuture.supplyAsync(() -> {
                    FetchResult fetch = fetchNdbcFileWithMeta(DEFAULT_BASE_URL, stationId, suffix);
                    if (fetch.success && fetch.content != null && !fetch.content.trim().isEmpty()) {
                        return suffix;
                    }
                    return null;
                }, taskExecutor))
                .collect(Collectors.toList());

        CompletableFuture<Void> all = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
        try {
            all.get(AVAILABLE_SUFFIX_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("并发探测可用后缀超时或异常，stationId={}", stationId, e);
        }

        return tasks.stream()
                .map(task -> task.getNow(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<DataSource> findStationsToSync() {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getStatus, 1)
                .eq(DataSource::getAutoSync, 1)
                .isNotNull(DataSource::getStationId);

        List<DataSource> stations = dataSourceService.list(wrapper);

        // 过滤需要同步的站点（根据同步间隔和最后同步时间）
        LocalDateTime now = LocalDateTime.now();
        return stations.stream()
                .filter(s -> {
                    if (s.getLastSyncTime() == null) {
                        return true;
                    }
                    int interval = s.getSyncIntervalMinutes() != null ? s.getSyncIntervalMinutes() : 30;
                    LocalDateTime nextSyncTime = s.getLastSyncTime().plusMinutes(interval);
                    return now.isAfter(nextSyncTime);
                })
                .collect(Collectors.toList());
    }

    /**
     * 解析文件后缀配置
     */
    private List<String> parseSuffixes(String fileSuffixes) {
        if (fileSuffixes == null || fileSuffixes.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(fileSuffixes.split(","))
                .map(String::trim)
                .map(s -> s.replaceFirst("^\\.", ""))
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .filter(SUPPORTED_SUFFIXES::contains)
                .collect(Collectors.toList());
    }

    private String normalizeNdbcBaseUrl(String baseUrl) {
        String value = StringUtils.hasText(baseUrl) ? baseUrl.trim() : DEFAULT_BASE_URL;
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith("/data")) {
            return value + "/realtime2";
        }
        return value;
    }

    /**
     * 加载变量名到数据类型的映射
     */
    private Map<String, ObservationDataType> loadVariableTypeMap() {
        long now = System.currentTimeMillis();
        CacheEntry<Map<String, ObservationDataType>> cached = variableTypeMapCache;
        if (cached != null && cached.expireAtMs > now) {
            return cached.value;
        }

        List<ObservationDataType> types = observationDataTypeService.list();
        Map<String, ObservationDataType> map = new HashMap<>();
        for (ObservationDataType type : types) {
            if (type.getNoaaVariableName() != null && !type.getNoaaVariableName().trim().isEmpty()) {
                map.put(type.getNoaaVariableName().toUpperCase(), type);
            }
        }
        variableTypeMapCache = new CacheEntry<>(map, now + VARIABLE_TYPE_CACHE_TTL_MS);
        return map;
    }

    private static class CacheEntry<T> {
        private final T value;
        private final long expireAtMs;

        private CacheEntry(T value, long expireAtMs) {
            this.value = value;
            this.expireAtMs = expireAtMs;
        }
    }

    private static class FetchResult {
        private final boolean success;
        private final String content;
        private final String errorType;
        private final String message;
        private final int attempts;
        private final long costMs;

        private FetchResult(boolean success, String content, String errorType, String message, int attempts, long costMs) {
            this.success = success;
            this.content = content;
            this.errorType = errorType;
            this.message = message;
            this.attempts = attempts;
            this.costMs = costMs;
        }

        private static FetchResult success(String content, int attempts, long costMs) {
            return new FetchResult(true, content, null, null, attempts, costMs);
        }

        private static FetchResult fail(String errorType, String message, int attempts, long costMs) {
            return new FetchResult(false, null, errorType, message, attempts, costMs);
        }
    }

    /**
     * 解析NDBC文件内容
     */
    private List<ObservationData> parseNdbcContent(String content,
                                                   Long dataSourceId,
                                                   BigDecimal stationLon,
                                                   BigDecimal stationLat,
                                                   Map<String, ObservationDataType> variableTypeMap) {
        List<ObservationData> result = new ArrayList<>();
        String[] lines = content.split("\n");
        String[] columns = null;
        int depthColumnIndex = -1; // DEPTH列的索引
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 解析表头
            if (trimmed.startsWith("#")) {
                if (columns == null) {
                    String header = trimmed.substring(1).trim();
                    columns = header.split("\\s+");
                    // 查找DEPTH列的位置
                    for (int i = 0; i < columns.length; i++) {
                        if ("DEPTH".equalsIgnoreCase(columns[i])) {
                            depthColumnIndex = i;
                            break;
                        }
                    }
                }
                continue;
            }

            if (columns == null) {
                continue;
            }

            String[] values = trimmed.split("\\s+");
            if (values.length < 5) {
                continue;
            }

            try {
                // 解析时间
                int year = Integer.parseInt(values[0]);
                int month = Integer.parseInt(values[1]);
                int day = Integer.parseInt(values[2]);
                int hour = Integer.parseInt(values[3]);
                int minute = Integer.parseInt(values[4]);
                LocalDateTime observationTime = LocalDateTime.of(year, month, day, hour, minute);

                // 提取DEPTH值（如果存在）
                BigDecimal depthValue = null;
                if (depthColumnIndex >= 0 && depthColumnIndex < values.length) {
                    String depthStr = values[depthColumnIndex];
                    if (!"MM".equalsIgnoreCase(depthStr)) {
                        try {
                            depthValue = new BigDecimal(depthStr);
                        } catch (NumberFormatException e) {
                            // 忽略无效的深度值
                        }
                    }
                }

                // 解析每个变量
                for (int i = 5; i < Math.min(columns.length, values.length); i++) {
                    String variableName = columns[i].toUpperCase();
                    
                    // DEPTH列已单独处理，不作为数据值入库
                    if ("DEPTH".equalsIgnoreCase(variableName)) {
                        continue;
                    }
                    
                    ObservationDataType dataType = variableTypeMap.get(variableName);

                    if (dataType == null) {
                        // 该变量没有配置对应的数据类型，跳过
                        continue;
                    }

                    String valueStr = values[i];
                    if ("MM".equalsIgnoreCase(valueStr) || "99.0".equals(valueStr)
                            || "999.0".equals(valueStr) || "9999.0".equals(valueStr)) {
                        // 缺测值
                        continue;
                    }

                    BigDecimal dataValue;
                    try {
                        dataValue = new BigDecimal(valueStr);
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    ObservationData data = new ObservationData();
                    data.setDataSourceId(dataSourceId);
                    data.setDataTypeId(dataType.getId());
                    data.setObservationTime(observationTime);
                    data.setDataValue(dataValue);

                    if (stationLon != null && stationLat != null) {
                        data.setLongitude(stationLon);
                        data.setLatitude(stationLat);
                    }
                    
                    // 设置深度值
                    if (depthValue != null) {
                        data.setDepth(depthValue);
                    }

                    // 生成唯一ID（包含深度信息以区分不同深度的数据）
                    String depthSuffix = depthValue != null ? "_D" + depthValue.toPlainString() : "";
                    String apiDataId = String.format("NDBC_%d_%s_%s%s",
                            dataSourceId,
                            observationTime.format(timeFormatter),
                            variableName,
                            depthSuffix);
                    data.setApiDataId(apiDataId);

                    result.add(data);
                }
            } catch (Exception e) {
                log.debug("解析行数据失败: {}", trimmed);
            }
        }

        return result;
    }

    /**
     * 从内容中提取变量列表
     */
    private List<String> extractVariables(String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                String header = trimmed.substring(1).trim();
                String[] columns = header.split("\\s+");
                if (columns.length > 5) {
                    return Arrays.asList(Arrays.copyOfRange(columns, 5, columns.length));
                }
            }
        }
        return Collections.emptyList();
    }
}

package com.boot.study.controller.admin;

import com.boot.study.bean.Result;
import com.boot.study.entity.DataSource;
import com.boot.study.enums.ResultEnum;
import com.boot.study.utils.PathLongParser;
import com.boot.study.entity.ObservationData;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * NOAA数据同步管理控制器（文件上传 + HTTP自动采集）
 * <p>
 * 支持两种数据导入方式：
 * 1. 文件上传：上传 NOAA / NDBC 文本文件导入观测数据
 * 2. HTTP自动采集：通过API自动拉取NDBC站点数据
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/noaa")
@RequiredArgsConstructor
public class NoaaDataSyncController {

    private final NoaaDataService noaaDataService;
    private final DataSourceService dataSourceService;
    private final ObservationDataTypeService observationDataTypeService;
    private final DataQualityService dataQualityService;
    private final NdbcAutoSyncService ndbcAutoSyncService;
    private final DataSourceAutoGenerateService dataSourceAutoGenerateService;

    /**
     * 上传 NDBC 文本文件并导入观测数据
     * <p>
     * 支持 NOAA NDBC 的 realtime2 文本格式，例如 32ST0.txt。
     * 通过 dataTypeCode 指定要导入的观测变量类型（如：TEMP、WTMP 等），
     * 通过 variable 指定文件中的列名（如：WTMP）。
     *
     * @param dataSourceId  数据源ID
     * @param variable      文件中的列名（如：WTMP）
     * @param dataTypeCode  系统中配置的数据类型编码（如：TEMP）
     * @param file          上传的文本文件
     * @return 导入结果（总记录数 / 成功保存 / 重复）
     */
    @PostMapping("/upload-ndbc-text")
    public Result<Map<String, Object>> uploadNdbcText(
            @RequestParam Long dataSourceId,
            @RequestParam String variable,
            @RequestParam String dataTypeCode,
            @RequestPart("file") MultipartFile file) {

        log.info("接收 NDBC 文本文件上传，dataSourceId: {}，variable: {}，dataTypeCode: {}，file: {}",
                dataSourceId, variable, dataTypeCode, file != null ? file.getOriginalFilename() : null);

        try {
            if (file == null || file.isEmpty()) {
                return Result.fail(400, "文件不能为空");
            }

            // 校验数据源
            DataSource dataSource = dataSourceService.getById(dataSourceId);
            if (dataSource == null) {
                return Result.fail(400, "数据源不存在");
            }
            if (dataSource.getStatus() == null || dataSource.getStatus() != 1) {
                return Result.fail(400, "数据源未启用");
            }

            // 校验数据类型
            Long dataTypeId = findDataTypeIdByCode(dataTypeCode);
            if (dataTypeId == null) {
                return Result.fail(400, "未找到对应的数据类型: " + dataTypeCode);
            }

            // 从数据源配置中尝试读取站点经纬度（可选）
            // 约定 configJson 为形如：
            // {"stationId":"32ST0","longitude":"120.123","latitude":"30.456"}
            // 其中 longitude/latitude 为字符串形式的经纬度，便于前端直接配置
            BigDecimal stationLon = null;
            BigDecimal stationLat = null;
            if (dataSource.getConfigJson() != null && !dataSource.getConfigJson().trim().isEmpty()) {
                try {
                    com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(dataSource.getConfigJson());
                    String lonStr = json.getString("longitude");
                    if (lonStr == null) {
                        lonStr = json.getString("lon");
                    }
                    String latStr = json.getString("latitude");
                    if (latStr == null) {
                        latStr = json.getString("lat");
                    }
                    if (lonStr != null && !lonStr.trim().isEmpty()) {
                        stationLon = new BigDecimal(lonStr.trim());
                    }
                    if (latStr != null && !latStr.trim().isEmpty()) {
                        stationLat = new BigDecimal(latStr.trim());
                    }
                } catch (Exception e) {
                    log.warn("解析数据源 configJson 失败: {}", dataSource.getConfigJson(), e);
                }
            }

            // 解析文本文件
            List<ObservationData> dataList = parseNdbcTextFile(
                    file,
                    dataSourceId,
                    dataTypeId,
                    variable,
                    stationLon,
                    stationLat
            );

            // 质量检测
            ObservationDataType dataType = observationDataTypeService.getById(dataTypeId);
            Map<Long, ObservationDataType> typeMap = new HashMap<>();
            if (dataType != null) {
                typeMap.put(dataTypeId, dataType);
            }
            dataList = dataQualityService.evaluateBatch(dataList, typeMap);
            Map<String, Integer> qualityStats = dataQualityService.getQualityStatistics(dataList);

            // 批量保存数据（自动去重）
            int savedCount = 0;
            if (dataList != null && !dataList.isEmpty()) {
                savedCount = noaaDataService.saveBatchWithDeduplication(dataList);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalCrawled", dataList != null ? dataList.size() : 0);
            result.put("savedCount", savedCount);
            result.put("duplicateCount", (dataList != null ? dataList.size() : 0) - savedCount);
            result.put("qualityStats", qualityStats);

            log.info("NDBC 文本文件导入完成，数据源ID: {}，总记录: {}，保存: {}，重复: {}",
                    dataSourceId,
                    dataList != null ? dataList.size() : 0,
                    savedCount,
                    (dataList != null ? dataList.size() : 0) - savedCount);

            return Result.success(result);

        } catch (Exception e) {
            log.error("NDBC 文本文件导入失败，数据源ID: {}", dataSourceId, e);
            return Result.fail(400, "文件导入失败: " + e.getMessage());
        }
    }

    /**
     * 根据类型编码查找数据类型ID
     */
    private Long findDataTypeIdByCode(String dataTypeCode) {
        if (dataTypeCode == null || dataTypeCode.trim().isEmpty()) {
            return null;
        }
        ObservationDataType type = observationDataTypeService.getOne(
                Wrappers.<ObservationDataType>lambdaQuery()
                        .eq(ObservationDataType::getTypeCode, dataTypeCode.trim().toUpperCase())
                        .last("LIMIT 1")
        );
        return type != null ? type.getId() : null;
    }

    /**
     * 解析 NDBC 文本文件
     * <p>
     * 示例头部：
     * #YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS PTDY  TIDE
     * #yr  mo dy hr mn degT m/s  m/s     m   sec   sec degT   hPa  degC  degC  degC  nmi  hPa    ft
     * 
     * .ocean.txt 文件头部：
     * #YY  MM DD hh mm DEPTH OTMP COND SAL O2% O2PPM CLCON TURB PH EH
     */
    private List<ObservationData> parseNdbcTextFile(MultipartFile file,
                                                    Long dataSourceId,
                                                    Long dataTypeId,
                                                    String variable,
                                                    BigDecimal stationLon,
                                                    BigDecimal stationLat) throws Exception {
        List<ObservationData> result = new java.util.ArrayList<>();
        int variableIndex = -1;
        int depthIndex = -1; // DEPTH列的索引
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                // 头部行：解析列名，找到目标变量所在列和DEPTH列
                if (trimmed.startsWith("#")) {
                    if (variableIndex < 0 && trimmed.contains(variable)) {
                        String header = trimmed.substring(1).trim();
                        String[] cols = header.split("\\s+");
                        for (int i = 0; i < cols.length; i++) {
                            if (cols[i].equalsIgnoreCase(variable)) {
                                variableIndex = i;
                            }
                            if (cols[i].equalsIgnoreCase("DEPTH")) {
                                depthIndex = i;
                            }
                        }
                        if (variableIndex < 0) {
                            log.warn("在头部行中未找到变量 [{}]，header: {}", variable, header);
                        }
                    }
                    continue;
                }

                if (variableIndex < 0) {
                    // 还未识别到列名行，继续跳过
                    continue;
                }

                String[] cols = trimmed.split("\\s+");
                // 前 5 列必须为时间字段
                if (cols.length <= Math.max(4, variableIndex)) {
                    continue;
                }

                try {
                    int year = Integer.parseInt(cols[0]);
                    int month = Integer.parseInt(cols[1]);
                    int day = Integer.parseInt(cols[2]);
                    int hour = Integer.parseInt(cols[3]);
                    int minute = Integer.parseInt(cols[4]);

                    String valueStr = cols[variableIndex];
                    if ("MM".equalsIgnoreCase(valueStr)) {
                        // 缺测
                        continue;
                    }

                    BigDecimal dataValue = new BigDecimal(valueStr);
                    LocalDateTime observationTime = LocalDateTime.of(year, month, day, hour, minute);

                    // 提取DEPTH值（如果存在）
                    BigDecimal depthValue = null;
                    if (depthIndex >= 0 && depthIndex < cols.length) {
                        String depthStr = cols[depthIndex];
                        if (!"MM".equalsIgnoreCase(depthStr)) {
                            try {
                                depthValue = new BigDecimal(depthStr);
                            } catch (NumberFormatException e) {
                                // 忽略无效的深度值
                            }
                        }
                    }

                    ObservationData data = new ObservationData();
                    data.setDataSourceId(dataSourceId);
                    data.setDataTypeId(dataTypeId);
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

                    data.setQualityFlag("GOOD");

                    // 生成唯一ID（包含深度信息）
                    String depthSuffix = depthValue != null ? "_D" + depthValue.toPlainString() : "";
                    String apiDataId = String.format("UPLOAD_NDBC_%d_%s_%s%s",
                            dataSourceId,
                            observationTime.format(timeFormatter),
                            variable.toUpperCase(),
                            depthSuffix);
                    data.setApiDataId(apiDataId);

                    result.add(data);
                } catch (Exception ex) {
                    log.warn("解析第 {} 行 NDBC 数据失败，内容: {}", lineNum, trimmed, ex);
                }
            }
        }

        return result;
    }

    // ==================== HTTP自动采集接口 ====================

    /**
     * 手动触发单个站点同步
     *
     * @param dataSourceId 数据源ID
     * @return 同步结果
     */
    @PostMapping("/sync/{dataSourceId}")
    public Result<Map<String, Object>> syncStation(@PathVariable("dataSourceId") String dataSourceIdStr) {
        Long dataSourceId = PathLongParser.tryParse(dataSourceIdStr);
        if (dataSourceId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "数据源ID无效");
        }
        log.info("手动触发站点同步，dataSourceId: {}", dataSourceId);

        try {
            DataSource dataSource = dataSourceService.getById(dataSourceId);
            if (dataSource == null) {
                return Result.fail(400, "数据源不存在");
            }
            if (dataSource.getStatus() == null || dataSource.getStatus() != 1) {
                return Result.fail(400, "数据源未启用");
            }
            if (dataSource.getStationId() == null || dataSource.getStationId().trim().isEmpty()) {
                return Result.fail(400, "站点ID未配置");
            }

            Map<String, Object> result = ndbcAutoSyncService.syncStation(dataSource);
            return Result.success(result);

        } catch (Exception e) {
            log.error("站点同步失败，dataSourceId: {}", dataSourceId, e);
            return Result.fail(400, "同步失败: " + e.getMessage());
        }
    }

    /**
     * 同步所有启用自动同步的站点
     *
     * @return 同步结果汇总
     */
    @PostMapping("/sync-all")
    public Result<Map<String, Object>> syncAllStations() {
        log.info("手动触发全部站点同步");

        try {
            Map<String, Object> result = ndbcAutoSyncService.syncAllEnabledStations();
            return Result.success(result);

        } catch (Exception e) {
            log.error("全部站点同步失败", e);
            return Result.fail(400, "同步失败: " + e.getMessage());
        }
    }

    /**
     * 预览远程NDBC数据（不入库）
     *
     * @param dataSourceId 数据源ID
     * @return 预览结果
     */
    @GetMapping("/preview/{dataSourceId}")
    public Result<Map<String, Object>> previewRemoteData(@PathVariable("dataSourceId") String dataSourceIdStr) {
        Long dataSourceId = PathLongParser.tryParse(dataSourceIdStr);
        if (dataSourceId == null) {
            return Result.fail(ResultEnum.PARAM_ERROR.getCode(), "数据源ID无效");
        }
        log.info("预览远程NDBC数据，dataSourceId: {}", dataSourceId);

        try {
            Map<String, Object> result = ndbcAutoSyncService.previewRemoteData(dataSourceId);
            if (result.containsKey("error")) {
                return Result.fail(400, (String) result.get("error"));
            }
            return Result.success(result);

        } catch (Exception e) {
            log.error("预览远程数据失败，dataSourceId: {}", dataSourceId, e);
            return Result.fail(400, "预览失败: " + e.getMessage());
        }
    }

    /**
     * 查询站点可用的文件类型
     *
     * @param stationId 站点ID
     * @return 可用的文件后缀列表
     */
    @GetMapping("/available-files/{stationId}")
    public Result<List<String>> getAvailableFileSuffixes(@PathVariable String stationId) {
        log.info("查询站点可用文件类型，stationId: {}", stationId);

        try {
            List<String> suffixes = ndbcAutoSyncService.getAvailableFileSuffixes(stationId);
            return Result.success(suffixes);

        } catch (Exception e) {
            log.error("查询站点可用文件类型失败，stationId: {}", stationId, e);
            return Result.fail(400, "查询失败: " + e.getMessage());
        }
    }

    // ==================== NDBC 自动发现（Module A + B） ====================

    /**
     * 扫描 NDBC realtime2 目录，发现所有站点及其可用文件（Module A）
     */
    @GetMapping("/discover-stations")
    public Result<List<Map<String, Object>>> discoverStations() {
        log.info("开始扫描 NDBC realtime2 目录");
        try {
            String url = "https://www.ndbc.noaa.gov/data/realtime2/";
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 OceanDataSystem/1.0");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<String> resp = rt.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);
            String html = resp.getBody();
            if (html == null) return Result.fail(500, "NDBC 返回空内容");

            // href="XXXXX.yyy"
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "href=\"([A-Za-z0-9]+)\\.(txt|spec|ocean|rain|srad|supl|cwind|adcp|swdir|swdir2|swr1|swr2|hkp|dart)\"",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(html);
            Map<String, Set<String>> byStation = new HashMap<>();
            while (m.find()) {
                String station = m.group(1).toUpperCase();
                String suffix = m.group(2).toLowerCase();
                byStation.computeIfAbsent(station, k -> new HashSet<>()).add(suffix);
            }

            List<Map<String, Object>> out = new ArrayList<>();
            for (Map.Entry<String, Set<String>> e : byStation.entrySet()) {
                Map<String, Object> row = new HashMap<>();
                row.put("stationId", e.getKey());
                row.put("availableSuffixes", new ArrayList<>(e.getValue()));
                row.put("hasWaveData", e.getValue().contains("spec"));
                out.add(row);
            }
            out.sort((a, b) -> ((String) a.get("stationId")).compareTo((String) b.get("stationId")));
            log.info("NDBC 站点扫描完成: {} 个", out.size());
            return Result.success(out);
        } catch (Exception e) {
            log.error("NDBC 目录扫描失败", e);
            return Result.fail(500, "扫描失败: " + e.getMessage());
        }
    }

    /**
     * 抓取某站点 station_page.php 元数据（Module B）
     */
    @GetMapping("/station-meta")
    public Result<Map<String, Object>> stationMeta(@org.springframework.web.bind.annotation.RequestParam String stationId) {
        if (stationId == null || stationId.trim().isEmpty()) {
            return Result.fail(400, "stationId 不能为空");
        }
        try {
            Map<String, Object> meta = fetchStationMeta(stationId.trim());
            if (meta == null) return Result.fail(500, "空响应");
            return Result.success(meta);
        } catch (Exception e) {
            log.error("抓取站点元数据失败 stationId={}", stationId, e);
            return Result.fail(500, "抓取失败: " + e.getMessage());
        }
    }

    /**
     * 内部：抓取并解析 station_page.php（批量流程复用）
     */
    private Map<String, Object> fetchStationMeta(String stationId) throws Exception {
        String sid = stationId;
        String url = "https://www.ndbc.noaa.gov/station_page.php?station=" + sid.toLowerCase();
        org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 OceanDataSystem/1.0");
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
        org.springframework.http.ResponseEntity<String> resp = rt.exchange(
                url, org.springframework.http.HttpMethod.GET, entity, String.class);
        String html = resp.getBody();
        if (html == null) return null;

        Map<String, Object> meta = new HashMap<>();
        meta.put("stationId", sid.toUpperCase());

        java.util.regex.Matcher nm = java.util.regex.Pattern
                .compile("<h1[^>]*>\\s*Station\\s+\\S+\\s*-\\s*([^<]+)</h1>", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html);
        if (nm.find()) meta.put("stationName", nm.group(1).trim());

        java.util.regex.Matcher lm = java.util.regex.Pattern
                .compile("(-?\\d+\\.\\d+)\\s*([NS])\\s*(-?\\d+\\.\\d+)\\s*([EW])")
                .matcher(html);
        if (lm.find()) {
            double lat = Double.parseDouble(lm.group(1));
            if ("S".equalsIgnoreCase(lm.group(2))) lat = -lat;
            double lon = Double.parseDouble(lm.group(3));
            if ("W".equalsIgnoreCase(lm.group(4))) lon = -lon;
            meta.put("latitude", lat);
            meta.put("longitude", lon);
        }

        java.util.regex.Matcher dm = java.util.regex.Pattern
                .compile("Water depth:?\\s*</?[^>]*>?\\s*([\\d.]+\\s*m)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html);
        if (dm.find()) meta.put("waterDepth", dm.group(1).trim());

        java.util.regex.Matcher om = java.util.regex.Pattern
                .compile("Owner:?\\s*</?[^>]*>?\\s*([^<\\n]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html);
        if (om.find()) meta.put("owner", om.group(1).trim());

        meta.put("description", buildDescription(meta));
        return meta;
    }

    private String buildDescription(Map<String, Object> meta) {
        StringBuilder sb = new StringBuilder();
        if (meta.get("stationName") != null) sb.append(meta.get("stationName")).append(" | ");
        if (meta.get("waterDepth") != null) sb.append("水深: ").append(meta.get("waterDepth")).append(" | ");
        if (meta.get("owner") != null) sb.append("所属: ").append(meta.get("owner"));
        return sb.toString();
    }

    /**
     * 批量创建 NDBC 数据源（Module A 配套）
     */
    @PostMapping("/batch-create-sources")
    public Result<Map<String, Object>> batchCreateSources(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> stationIds = (List<String>) body.get("stationIds");
        if (stationIds == null || stationIds.isEmpty()) {
            return Result.fail(400, "stationIds 不能为空");
        }

        boolean autoGenerate = Boolean.TRUE.equals(body.get("autoGenerate"));
        boolean syncData = Boolean.TRUE.equals(body.get("syncData"));

        int created = 0;
        int skipped = 0;
        int totalCharts = 0;
        int totalScenes = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (String sid : stationIds) {
            if (sid == null || sid.trim().isEmpty()) continue;
            String station = sid.trim().toUpperCase();

            DataSource exists = dataSourceService.getOne(Wrappers.<DataSource>lambdaQuery()
                    .eq(DataSource::getStationId, station).last("LIMIT 1"), false);
            if (exists != null) {
                skipped++;
                continue;
            }

            DataSource ds = new DataSource();
            ds.setSourceName("NOAA NDBC " + station);
            ds.setSourceType("NOAA");
            ds.setApiUrl("https://www.ndbc.noaa.gov/data/realtime2/");
            ds.setStationId(station);
            ds.setStatus(1);
            ds.setAutoSync(1);
            ds.setSyncIntervalMinutes(60);
            ds.setFileSuffixes("txt,spec,ocean");

            // 1. 抓取站点元数据，自动填充经纬度/描述/configJson
            String description = "NDBC 自动发现导入";
            try {
                Map<String, Object> meta = fetchStationMeta(station);
                if (meta != null) {
                    if (meta.get("latitude") != null && meta.get("longitude") != null) {
                        ds.setLatitude(BigDecimal.valueOf(((Number) meta.get("latitude")).doubleValue()));
                        ds.setLongitude(BigDecimal.valueOf(((Number) meta.get("longitude")).doubleValue()));
                    }
                    if (meta.get("stationName") != null) {
                        ds.setSourceName("NOAA NDBC " + station + " - " + meta.get("stationName"));
                    }
                    if (meta.get("description") != null) {
                        description = (String) meta.get("description");
                    }
                    com.alibaba.fastjson2.JSONObject cfg = new com.alibaba.fastjson2.JSONObject();
                    cfg.put("stationId", station);
                    if (meta.get("latitude") != null) cfg.put("latitude", String.valueOf(meta.get("latitude")));
                    if (meta.get("longitude") != null) cfg.put("longitude", String.valueOf(meta.get("longitude")));
                    if (meta.get("waterDepth") != null) cfg.put("waterDepth", meta.get("waterDepth"));
                    if (meta.get("owner") != null) cfg.put("owner", meta.get("owner"));
                    if (meta.get("stationName") != null) cfg.put("stationName", meta.get("stationName"));
                    ds.setConfigJson(cfg.toJSONString());
                }
            } catch (Exception ex) {
                log.warn("抓取站点元数据失败，使用默认值。station={}", station, ex);
            }
            ds.setDescription(description);

            dataSourceService.save(ds);
            created++;

            Map<String, Object> detail = new HashMap<>();
            detail.put("stationId", station);
            detail.put("dataSourceId", ds.getId());

            // 2. 可选：拉一次远程数据
            if (syncData && ds.getId() != null) {
                try {
                    Map<String, Object> syncResult = ndbcAutoSyncService.syncStation(ds);
                    detail.put("synced", syncResult);
                } catch (Exception ex) {
                    log.warn("同步数据失败 station={}, dsId={}", station, ds.getId(), ex);
                    detail.put("syncError", ex.getMessage());
                }
            }

            // 3. 可选：一键生成图表/场景
            if (autoGenerate && ds.getId() != null) {
                try {
                    Map<String, Object> gen = dataSourceAutoGenerateService.generate(ds.getId());
                    Object cc = gen.get("createdCharts");
                    Object sc = gen.get("createdScenes");
                    if (cc instanceof Number) totalCharts += ((Number) cc).intValue();
                    if (sc instanceof Number) totalScenes += ((Number) sc).intValue();
                    detail.put("autoGen", gen);
                } catch (Exception ex) {
                    log.warn("自动生成失败 station={}, dsId={}", station, ds.getId(), ex);
                    detail.put("autoGenError", ex.getMessage());
                }
            }
            details.add(detail);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("totalCharts", totalCharts);
        result.put("totalScenes", totalScenes);
        result.put("details", details);
        return Result.success(result);
    }

    /**
     * 获取数据类型与NOAA变量的映射关系
     *
     * @return 映射列表
     */
    @GetMapping("/variable-mapping")
    public Result<List<Map<String, Object>>> getVariableMapping() {
        try {
            List<ObservationDataType> types = observationDataTypeService.list();
            List<Map<String, Object>> result = types.stream()
                    .filter(t -> t.getNoaaVariableName() != null && !t.getNoaaVariableName().trim().isEmpty())
                    .map(t -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("typeId", t.getId());
                        map.put("typeCode", t.getTypeCode());
                        map.put("typeName", t.getTypeName());
                        map.put("noaaVariableName", t.getNoaaVariableName());
                        map.put("unit", t.getUnit());
                        return map;
                    })
                    .collect(Collectors.toList());
            return Result.success(result);

        } catch (Exception e) {
            log.error("获取变量映射关系失败", e);
            return Result.fail(400, "获取失败: " + e.getMessage());
        }
    }
}


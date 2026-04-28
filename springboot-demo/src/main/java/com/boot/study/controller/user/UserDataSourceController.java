package com.boot.study.controller.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boot.study.bean.Result;
import com.boot.study.entity.ChartConfig;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.VisualizationScene;
import com.boot.study.enums.PublicFlagEnum;
import com.boot.study.service.ChartConfigService;
import com.boot.study.service.DataSourceService;
import com.boot.study.service.ObservationDataService;
import com.boot.study.service.VisualizationSceneService;
import com.boot.study.utils.PathLongParser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/user/data-source")
@RequiredArgsConstructor
public class UserDataSourceController {

    private final DataSourceService dataSourceService;
    private final ChartConfigService chartConfigService;
    private final VisualizationSceneService visualizationSceneService;
    private final ObservationDataService observationDataService;
    private final RestTemplate restTemplate;

    @GetMapping("/list")
    public Result<List<DataSource>> list() {
        List<DataSource> list = dataSourceService.list(
                Wrappers.<DataSource>lambdaQuery()
                        .eq(DataSource::getStatus, 1)
                        .orderByAsc(DataSource::getSourceName)
        );
        return Result.success(list);
    }

    /**
     * BuoyCAM 页面解析：NDBC 文档提供的是 buoycam.php 页面 URL，不是稳定图片直链。
     * 这里尽力解析真实图片地址；解析不到时前端应只展示“在 NDBC 打开”链接。
     */
    @GetMapping("/{id}/buoycam-image")
    public Result<Map<String, Object>> buoyCamImage(@PathVariable("id") String idStr) {
        Long id = PathLongParser.tryParse(idStr);
        Map<String, Object> data = new HashMap<>();
        data.put("available", false);
        if (id == null) {
            data.put("message", "数据源ID无效");
            return Result.success(data);
        }
        DataSource ds = dataSourceService.getById(id);
        if (ds == null || !StringUtils.hasText(ds.getStationId())) {
            data.put("message", "站点不存在或缺少 stationId");
            return Result.success(data);
        }
        String pageUrl = resolveBuoyCamUrl(ds);
        data.put("pageUrl", pageUrl);
        try {
            String html = restTemplate.getForObject(pageUrl, String.class);
            String imageUrl = extractBuoyCamImageUrl(pageUrl, html);
            if (StringUtils.hasText(imageUrl)) {
                data.put("available", true);
                data.put("imageUrl", imageUrl);
                data.put("message", "OK");
            } else {
                data.put("message", resolveBuoyCamMessage(html));
            }
        } catch (Exception e) {
            data.put("message", "暂时无法访问 NDBC BuoyCAM");
        }
        return Result.success(data);
    }

    /**
     * 用户端首页统计：场景/图表/观测数据条数
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        long sceneCount = visualizationSceneService.count(
                Wrappers.<VisualizationScene>lambdaQuery()
                        .eq(VisualizationScene::getIsPublic, PublicFlagEnum.YES));
        long chartCount = chartConfigService.count(
                Wrappers.<ChartConfig>lambdaQuery()
                        .eq(ChartConfig::getIsPublic, PublicFlagEnum.YES));
        long dataCount = observationDataService.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("sceneCount", sceneCount);
        stats.put("chartCount", chartCount);
        stats.put("dataCount", dataCount);
        return Result.success(stats);
    }

    /**
     * Module H - 地图视图所需：站点 + 关联图表/场景数量
     */
    @GetMapping("/map-stations")
    public Result<List<Map<String, Object>>> mapStations() {
        List<DataSource> all = dataSourceService.list(
                Wrappers.<DataSource>lambdaQuery()
                        .eq(DataSource::getStatus, 1));

        // 所有公开图表和场景（在内存中按 dataQueryConfig/configJson 中的 dataSourceId 聚合）
        List<ChartConfig> charts = chartConfigService.list(
                Wrappers.<ChartConfig>lambdaQuery().eq(ChartConfig::getIsPublic, PublicFlagEnum.YES));
        List<VisualizationScene> scenes = visualizationSceneService.list(
                Wrappers.<VisualizationScene>lambdaQuery().eq(VisualizationScene::getIsPublic, PublicFlagEnum.YES));

        List<Map<String, Object>> result = new ArrayList<>();
        for (DataSource ds : all) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", ds.getId());
            row.put("stationId", ds.getStationId());
            row.put("name", ds.getSourceName());
            row.put("sourceType", ds.getSourceType());
            row.put("stationType", stationTypeCode(ds));
            row.put("stationTypeDesc", stationTypeDesc(ds));
            row.put("longitude", ds.getLongitude());
            row.put("latitude", ds.getLatitude());
            row.put("description", ds.getDescription());
            row.put("fileSuffixes", ds.getFileSuffixes());
            row.put("apiUrl", ds.getApiUrl());
            row.put("officialUrl", resolveOfficialUrl(ds));
            row.put("historyUrl", resolveHistoryUrl(ds));
            row.put("buoyCamUrl", resolveBuoyCamUrl(ds));
            row.put("latestObsUrl", "https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt");
            row.put("autoSync", ds.getAutoSync());

            String oceanRegion = resolveOceanRegion(ds.getLatitude(), ds.getLongitude());
            row.put("oceanRegion", oceanRegion);
            row.put("oceanRegionDesc", resolveOceanRegionDesc(oceanRegion));
            row.put("programOwner", resolveProgramOwner(ds));

            List<Map<String, Object>> relatedCharts = new ArrayList<>();
            for (ChartConfig c : charts) {
                if (matchesDataSource(c.getDataQueryConfig(), ds.getId(), false)) {
                    Map<String, Object> cm = new HashMap<>();
                    cm.put("id", c.getId());
                    cm.put("name", c.getChartName());
                    relatedCharts.add(cm);
                }
            }
            List<Map<String, Object>> relatedScenes = new ArrayList<>();
            for (VisualizationScene s : scenes) {
                if (matchesDataSource(s.getConfigJson(), ds.getId(), true)) {
                    Map<String, Object> sm = new HashMap<>();
                    sm.put("id", s.getId());
                    sm.put("name", s.getSceneName());
                    relatedScenes.add(sm);
                }
            }
            row.put("charts", relatedCharts);
            row.put("scenes", relatedScenes);
            row.put("chartCount", relatedCharts.size());
            row.put("sceneCount", relatedScenes.size());
            row.put("hasWaveData", hasWaveData(ds, relatedCharts.size()));
            result.add(row);
        }
        return Result.success(result);
    }

    private boolean hasWaveData(DataSource ds, int chartCount) {
        String suffix = ds.getFileSuffixes() == null ? "" : ds.getFileSuffixes().toLowerCase();
        return suffix.contains("spec")
                || suffix.contains("data_spec")
                || suffix.contains("wave")
                || suffix.contains("swden")
                || suffix.contains("wvht")
                || chartCount > 0;
    }

    private String resolveOfficialUrl(DataSource ds) {
        if (StringUtils.hasText(ds.getStationId())) {
            return "https://www.ndbc.noaa.gov/station_page.php?station=" + ds.getStationId().trim().toUpperCase();
        }
        return "";
    }

    private String resolveHistoryUrl(DataSource ds) {
        if (!StringUtils.hasText(ds.getStationId())) return "";
        return "https://www.ndbc.noaa.gov/station_history.php?station=" + ds.getStationId().trim().toUpperCase();
    }

    private String resolveBuoyCamUrl(DataSource ds) {
        if (!StringUtils.hasText(ds.getStationId())) return "";
        return "https://www.ndbc.noaa.gov/buoycam.php?station=" + ds.getStationId().trim().toUpperCase();
    }

    private String extractBuoyCamImageUrl(String pageUrl, String html) {
        if (!StringUtils.hasText(html)) return "";
        Pattern pattern = Pattern.compile("(?is)<img\\b[^>]*\\bsrc=[\"']([^\"']+(?:\\.jpg|\\.jpeg|\\.png|buoycam)[^\"']*)[\"'][^>]*>");
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String src = matcher.group(1);
            if (!StringUtils.hasText(src)) continue;
            String lower = src.toLowerCase();
            if (lower.contains("logo") || lower.contains("spacer") || lower.contains("button")) continue;
            try {
                return URI.create(pageUrl).resolve(src.trim()).toString();
            } catch (Exception ignored) {
                return src.trim();
            }
        }
        return "";
    }

    private String resolveBuoyCamMessage(String html) {
        if (!StringUtils.hasText(html)) return "该站点暂无 BuoyCAM 图片";
        String text = html.replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.contains("has no BuoyCAM")) return "该站点未安装 BuoyCAM";
        if (text.contains("older than 16 hours")) return "该站点 BuoyCAM 图片超过 16 小时";
        if (text.contains("Invalid station")) return "BuoyCAM 站点无效";
        if (text.contains("Unable to access")) return "NDBC 暂时无法访问 BuoyCAM";
        return "该站点暂无可内联展示的 BuoyCAM 图片";
    }

    /** 根据经纬度推断大洋归属。粒度：5 大洋 + 墨西哥湾 + 五大湖。 */
    private String resolveOceanRegion(java.math.BigDecimal lat, java.math.BigDecimal lon) {
        if (lat == null || lon == null) return "";
        double la = lat.doubleValue();
        double lo = lon.doubleValue();
        while (lo > 180) lo -= 360;
        while (lo <= -180) lo += 360;

        if (la >= 66.5) return "ARCTIC";
        if (la <= -60) return "SOUTHERN";
        // NDBC 主要覆盖区
        if (la >= 18 && la <= 31 && lo >= -98 && lo <= -80) return "GULF_OF_MEXICO";
        if (la >= 41 && la <= 49 && lo >= -93 && lo <= -76) return "GREAT_LAKES";
        if (la >= -60 && la <= 70 && lo >= -70 && lo <= 20) return "ATLANTIC";
        if (la >= -60 && la <= 30 && lo >= 20 && lo <= 100) return "INDIAN";
        return "PACIFIC";
    }

    private String resolveOceanRegionDesc(String code) {
        if (!StringUtils.hasText(code)) return "未知海域";
        switch (code) {
            case "ARCTIC": return "北冰洋";
            case "SOUTHERN": return "南冰洋";
            case "GULF_OF_MEXICO": return "墨西哥湾";
            case "GREAT_LAKES": return "五大湖";
            case "ATLANTIC": return "大西洋";
            case "INDIAN": return "印度洋";
            case "PACIFIC": return "太平洋";
            default: return "未知海域";
        }
    }

    /** 从 configJson 中提取来源机构。NDBC activestations.xml 的 owner 才是机构归属，program 仅作为兜底项目名。 */
    private String resolveProgramOwner(DataSource ds) {
        String desc = StringUtils.hasText(ds.getDescription()) ? ds.getDescription() : "";
        String config = ds.getConfigJson();
        if (StringUtils.hasText(config)) {
            try {
                com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(config);
                if (json != null) {
                    String owner = json.getString("owner");
                    String program = json.getString("program");
                    if (StringUtils.hasText(owner)) return owner;
                    if (StringUtils.hasText(program)) return program;
                }
            } catch (Exception ignored) {
                // 配置 JSON 不规范则使用描述兜底
            }
        }
        if ("NOAA".equalsIgnoreCase(ds.getSourceType())) return "NOAA / NDBC";
        return desc.length() > 40 ? desc.substring(0, 40) + "…" : desc;
    }

    private boolean matchesDataSource(String configJson, Long dataSourceId, boolean nestedDataQuery) {
        if (!StringUtils.hasText(configJson) || dataSourceId == null) return false;
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(configJson);
            if (json == null) return false;
            if (dataSourceId.equals(json.getLong("dataSourceId")) || dataSourceId.equals(json.getLong("autoGenSourceId"))) {
                return true;
            }
            if (nestedDataQuery) {
                com.alibaba.fastjson2.JSONObject dataQuery = json.getJSONObject("dataQuery");
                return dataQuery != null
                        && (dataSourceId.equals(dataQuery.getLong("dataSourceId"))
                        || dataSourceId.equals(dataQuery.getLong("autoGenSourceId")));
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private String stationTypeCode(DataSource ds) {
        String suffix = ds.getFileSuffixes() == null ? "" : ds.getFileSuffixes().toLowerCase();
        String config = ds.getConfigJson() == null ? "" : ds.getConfigJson().toLowerCase();
        if (suffix.contains("dart") || config.contains("\"dart\":\"y\"") || config.contains("\"dart\":true")) {
            return "DART";
        }
        if (suffix.contains("adcp") || config.contains("\"currents\":\"y\"") || config.contains("\"currents\":true")) {
            return "CURRENT";
        }
        if (suffix.contains("tide") || suffix.contains("wlevel")) {
            return "WATER_LEVEL";
        }
        if (suffix.contains("spec") || suffix.contains("data_spec") || suffix.contains("swden")) {
            return "WAVE";
        }
        if (suffix.contains("ocean") || config.contains("\"waterquality\":\"y\"") || config.contains("\"waterquality\":true")) {
            return "OCEAN";
        }
        if (suffix.contains("txt") || suffix.contains("cwind") || suffix.contains("rain") || suffix.contains("srad")
                || config.contains("\"met\":\"y\"") || config.contains("\"met\":true")) {
            return "MET";
        }
        if (StringUtils.hasText(ds.getSourceType())) {
            return ds.getSourceType().trim().toUpperCase();
        }
        return "UNKNOWN";
    }

    private String stationTypeDesc(DataSource ds) {
        switch (stationTypeCode(ds)) {
            case "DART":
                return "DART 海啸/水柱高度站";
            case "CURRENT":
                return "海流剖面观测站";
            case "WATER_LEVEL":
                return "潮位/水位观测站";
            case "WAVE":
                return "波浪谱观测站";
            case "OCEAN":
                return "海洋水质观测站";
            case "MET":
                return "标准气象观测站";
            case "ERDDAP":
                return "ERDDAP 科学数据站";
            case "CUSTOM":
                return "自定义站点";
            case "NOAA":
                return "NOAA NDBC 站点";
            default:
                return "未分类站点";
        }
    }
}


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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/data-source")
@RequiredArgsConstructor
public class UserDataSourceController {

    private final DataSourceService dataSourceService;
    private final ChartConfigService chartConfigService;
    private final VisualizationSceneService visualizationSceneService;
    private final ObservationDataService observationDataService;

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
            row.put("longitude", ds.getLongitude());
            row.put("latitude", ds.getLatitude());
            row.put("description", ds.getDescription());
            row.put("fileSuffixes", ds.getFileSuffixes());

            List<Map<String, Object>> relatedCharts = new ArrayList<>();
            for (ChartConfig c : charts) {
                if (c.getDataQueryConfig() != null && c.getDataQueryConfig().contains("\"" + ds.getId() + "\"")
                        || (c.getDataQueryConfig() != null && c.getDataQueryConfig().contains(":" + ds.getId() + ","))
                        || (c.getDataQueryConfig() != null && c.getDataQueryConfig().contains(":" + ds.getId() + "}"))) {
                    Map<String, Object> cm = new HashMap<>();
                    cm.put("id", c.getId());
                    cm.put("name", c.getChartName());
                    relatedCharts.add(cm);
                }
            }
            List<Map<String, Object>> relatedScenes = new ArrayList<>();
            for (VisualizationScene s : scenes) {
                if (s.getConfigJson() != null && (s.getConfigJson().contains(":" + ds.getId() + ",")
                        || s.getConfigJson().contains(":" + ds.getId() + "}"))) {
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
            result.add(row);
        }
        return Result.success(result);
    }
}


package com.boot.study.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.boot.study.entity.ChartConfig;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.ObservationData;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.entity.VisualizationScene;
import com.boot.study.enums.ChartTypeEnum;
import com.boot.study.enums.PublicFlagEnum;
import com.boot.study.enums.SceneTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据源一键生成图表 / 3D 场景服务（Module C 共享逻辑）
 * <p>
 * 可由管理员端手动按钮调用，也可由 NDBC 批量扫描导入流程自动触发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceAutoGenerateService {

    private final DataSourceService dataSourceService;
    private final ObservationDataService observationDataService;
    private final ObservationDataTypeService observationDataTypeService;
    private final ChartConfigService chartConfigService;
    private final VisualizationSceneService visualizationSceneService;

    /**
     * 为指定数据源生成默认图表集和 3D 场景。
     * 若该数据源暂无观测数据，仍会生成一个"空壳"3D 场景（用户同步数据后可见）。
     *
     * @param dsId 数据源 ID
     * @return 统计信息
     */
    public Map<String, Object> generate(Long dsId) {
        Map<String, Object> result = new HashMap<>();
        result.put("dataSourceId", dsId);
        result.put("createdCharts", 0);
        result.put("skippedCharts", 0);
        result.put("createdScenes", 0);
        result.put("typeCodes", new ArrayList<String>());

        DataSource ds = dataSourceService.getById(dsId);
        if (ds == null) {
            result.put("error", "数据源不存在");
            return result;
        }

        String sourceName = ds.getSourceName() != null ? ds.getSourceName() : ("Station-" + ds.getStationId());
        result.put("dataSourceName", sourceName);

        // 扫描已入库的 data_type_id（取前 2000 条样本去重）
        List<ObservationData> sample = observationDataService.list(
                Wrappers.<ObservationData>lambdaQuery()
                        .select(ObservationData::getDataTypeId)
                        .eq(ObservationData::getDataSourceId, dsId)
                        .last("LIMIT 2000"));
        Set<Long> typeIds = new HashSet<>();
        for (ObservationData od : sample) {
            if (od.getDataTypeId() != null) typeIds.add(od.getDataTypeId());
        }

        int createdCharts = 0;
        int skippedCharts = 0;
        List<String> createdTypeCodes = new ArrayList<>();

        if (!typeIds.isEmpty()) {
            List<ObservationDataType> types = observationDataTypeService.listByIds(typeIds);
            Map<Long, ObservationDataType> typeMap = new HashMap<>();
            for (ObservationDataType t : types) typeMap.put(t.getId(), t);

            for (Long typeId : typeIds) {
                ObservationDataType t = typeMap.get(typeId);
                if (t == null) continue;

                String chartName = sourceName + " - " +
                        (t.getTypeName() != null ? t.getTypeName() : t.getTypeCode()) + " 时间序列";

                ChartConfig existsChart = chartConfigService.getOne(
                        Wrappers.<ChartConfig>lambdaQuery()
                                .eq(ChartConfig::getChartName, chartName)
                                .last("LIMIT 1"), false);
                if (existsChart != null) {
                    skippedCharts++;
                    continue;
                }

                ChartConfig cc = new ChartConfig();
                cc.setChartName(chartName);
                cc.setChartType(ChartTypeEnum.LINE);
                cc.setIsPublic(PublicFlagEnum.YES);
                cc.setDescription("自动生成 | 来源: " + sourceName +
                        (ds.getStationId() != null ? " (" + ds.getStationId() + ")" : ""));

                JSONObject dq = new JSONObject();
                dq.put("dataSourceId", dsId);
                dq.put("dataSourceName", sourceName);
                dq.put("autoGenSourceId", dsId);
                dq.put("typeCodes", new String[]{t.getTypeCode()});
                dq.put("pageSize", 500);
                cc.setDataQueryConfig(dq.toJSONString());

                JSONObject ec = new JSONObject();
                ec.put("title", chartName);
                ec.put("yAxisLabel", t.getTypeName() +
                        (t.getUnit() != null ? " (" + t.getUnit() + ")" : ""));
                cc.setEchartsConfig(ec.toJSONString());

                chartConfigService.save(cc);
                createdCharts++;
                createdTypeCodes.add(t.getTypeCode());
            }
        }

        // 场景（无论是否有数据都创建一个空壳 3D 场景）
        String sceneName = sourceName + " - 3D 海洋场景";
        int createdScenes = 0;
        VisualizationScene existsScene = visualizationSceneService.getOne(
                Wrappers.<VisualizationScene>lambdaQuery()
                        .eq(VisualizationScene::getSceneName, sceneName)
                        .last("LIMIT 1"), false);
        if (existsScene == null) {
            VisualizationScene vs = new VisualizationScene();
            vs.setSceneName(sceneName);
            vs.setSceneType(SceneTypeEnum.OCEAN_3D);
            vs.setIsPublic(PublicFlagEnum.YES);
            vs.setDescription("自动生成 | 来源: " + sourceName);
            vs.setViewCount(0);

            JSONObject cfg = new JSONObject();
            JSONObject dataQuery = new JSONObject();
            dataQuery.put("dataSourceId", dsId);
            dataQuery.put("dataSourceName", sourceName);
            dataQuery.put("autoGenSourceId", dsId);
            cfg.put("dataQuery", dataQuery);

            JSONObject waveBinding = new JSONObject();
            waveBinding.put("waveHeightTypeCode", "WAVE_HEIGHT");
            waveBinding.put("wavePeriodTypeCode", "AVG_PERIOD");
            waveBinding.put("waveDirectionTypeCode", "WAVE_DIRECTION");
            waveBinding.put("waveSteepenessTypeCode", "WAVE_STEEPNESS");
            cfg.put("waveBinding", waveBinding);

            JSONObject camera = new JSONObject();
            if (ds.getLatitude() != null && ds.getLongitude() != null) {
                camera.put("target", new double[]{ds.getLatitude().doubleValue(), ds.getLongitude().doubleValue()});
            }
            camera.put("distance", 500000);
            cfg.put("camera", camera);

            cfg.put("layers", new Object[0]);
            vs.setConfigJson(cfg.toJSONString());
            visualizationSceneService.save(vs);
            createdScenes = 1;
        }

        result.put("createdCharts", createdCharts);
        result.put("skippedCharts", skippedCharts);
        result.put("createdScenes", createdScenes);
        result.put("typeCodes", createdTypeCodes);
        return result;
    }
}

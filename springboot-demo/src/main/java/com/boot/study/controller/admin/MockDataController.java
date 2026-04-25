package com.boot.study.controller.admin;

import com.boot.study.annotation.PassToken;
import com.boot.study.bean.Result;
import com.boot.study.entity.*;
import com.boot.study.enums.*;
import com.boot.study.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 假数据生成器 - 用于测试
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/mock-data")
public class MockDataController {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private ObservationDataTypeService observationDataTypeService;

    @Autowired
    private ObservationDataService observationDataService;

    @Autowired
    private VisualizationSceneService visualizationSceneService;

    @Autowired
    private ChartConfigService chartConfigService;

    @Autowired
    private SystemConfigService systemConfigService;

    private final Random random = new Random();

    /**
     * 生成所有假数据
     */
    @PostMapping("/generate-all")
    @PassToken
    public Result<String> generateAll() {
        try {
            // 1. 生成数据源
            List<DataSource> dataSources = generateDataSources();
            for (DataSource ds : dataSources) {
                dataSourceService.save(ds);
            }
            log.info("生成数据源: {} 条", dataSources.size());

            // 2. 生成观测数据类型
            List<ObservationDataType> dataTypes = generateObservationDataTypes();
            for (ObservationDataType dt : dataTypes) {
                observationDataTypeService.save(dt);
            }
            log.info("生成观测数据类型: {} 条", dataTypes.size());

            // 3. 生成观测数据
            List<ObservationData> observationDataList = generateObservationData(dataSources, dataTypes);
            observationDataService.saveBatch(observationDataList);
            log.info("生成观测数据: {} 条", observationDataList.size());

            // 4. 生成可视化场景
            List<VisualizationScene> scenes = generateVisualizationScenes();
            visualizationSceneService.saveBatch(scenes);
            log.info("生成可视化场景: {} 条", scenes.size());

            // 5. 生成图表配置
            List<ChartConfig> chartConfigs = generateChartConfigs();
            chartConfigService.saveBatch(chartConfigs);
            log.info("生成图表配置: {} 条", chartConfigs.size());

            // 6. 生成系统配置
            List<SystemConfig> systemConfigs = generateSystemConfigs();
            systemConfigService.saveBatch(systemConfigs);
            log.info("生成系统配置: {} 条", systemConfigs.size());

            return Result.success("假数据生成成功！共生成: " +
                    "数据源" + dataSources.size() + "条, " +
                    "观测数据类型" + dataTypes.size() + "条, " +
                    "观测数据" + observationDataList.size() + "条, " +
                    "可视化场景" + scenes.size() + "条, " +
                    "图表配置" + chartConfigs.size() + "条, " +
                    "系统配置" + systemConfigs.size() + "条");
        } catch (Exception e) {
            log.error("生成假数据失败", e);
            return Result.<String>fail(500, "生成假数据失败: " + e.getMessage());
        }
    }

    /**
     * 生成数据源
     */
    private List<DataSource> generateDataSources() {
        List<DataSource> list = new ArrayList<>();
        String[] names = {"NOAA海洋数据源", "ERDDAP全球数据", "太平洋观测站", "大西洋数据源", "印度洋监测站"};
        String[] types = {"NOAA", "ERDDAP", "CUSTOM", "NOAA", "ERDDAP"};
        String[] urls = {
                "https://www.ncei.noaa.gov/data/oceans/",
                "https://coastwatch.pfeg.noaa.gov/erddap/",
                "https://api.custom-ocean.com/v1/",
                "https://www.nodc.noaa.gov/",
                "https://erddap.marine.ie/erddap/"
        };

        for (int i = 0; i < names.length; i++) {
            DataSource ds = new DataSource();
            ds.setSourceName(names[i]);
            ds.setSourceType(types[i]);
            ds.setApiUrl(urls[i]);
            ds.setApiKey("mock_key_" + (i + 1));
            ds.setDescription("这是" + names[i] + "的描述信息");
            ds.setStatus(i % 2 == 0 ? 1 : 0);
            ds.setConfigJson("{\"timeout\":30,\"retry\":3}");
            list.add(ds);
        }
        return list;
    }

    /**
     * 生成观测数据类型
     */
    private List<ObservationDataType> generateObservationDataTypes() {
        List<ObservationDataType> list = new ArrayList<>();
        String[] names = {"海水温度", "海水盐度", "海流速度", "海浪高度", "溶解氧", "叶绿素浓度"};
        String[] units = {"°C", "PSU", "m/s", "m", "mg/L", "μg/L"};
        String[] codes = {"TEMP", "SAL", "CURRENT", "WAVE", "DO", "CHL"};

        for (int i = 0; i < names.length; i++) {
            ObservationDataType dt = new ObservationDataType();
            dt.setTypeName(names[i]);
            dt.setTypeCode(codes[i]);
            dt.setUnit(units[i]);
            dt.setDescription(names[i] + "观测数据类型");
            list.add(dt);
        }
        return list;
    }

    /**
     * 生成观测数据
     */
    private List<ObservationData> generateObservationData(List<DataSource> dataSources, List<ObservationDataType> dataTypes) {
        List<ObservationData> list = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);
        String[] qualityFlags = {"GOOD", "QUESTIONABLE", "BAD"};

        // 为每个数据源和数据类型组合生成多条数据
        for (DataSource ds : dataSources) {
            for (ObservationDataType dt : dataTypes) {
                // 每个组合生成10条数据
                for (int i = 0; i < 10; i++) {
                    ObservationData od = new ObservationData();
                    od.setDataSourceId(ds.getId());
                    od.setDataTypeId(dt.getId());
                    od.setObservationTime(baseTime.plusHours(i * 2));
                    
                    // 根据数据类型生成不同的数据值范围
                    BigDecimal value = generateDataValue(dt.getTypeCode());
                    od.setDataValue(value);
                    
                    // 生成经纬度（中国近海区域）
                    od.setLongitude(BigDecimal.valueOf(110 + random.nextDouble() * 20)); // 110-130°E
                    od.setLatitude(BigDecimal.valueOf(15 + random.nextDouble() * 15)); // 15-30°N
                    od.setDepth(BigDecimal.valueOf(random.nextDouble() * 5000)); // 0-5000米
                    od.setQualityFlag(qualityFlags[random.nextInt(qualityFlags.length)]);
                    od.setApiDataId("api_" + System.currentTimeMillis() + "_" + i);
                    od.setRemark("模拟观测数据 " + (i + 1));
                    list.add(od);
                }
            }
        }
        return list;
    }

    /**
     * 根据数据类型生成数据值
     */
    private BigDecimal generateDataValue(String typeCode) {
        switch (typeCode) {
            case "TEMP": // 温度 0-30°C
                return BigDecimal.valueOf(random.nextDouble() * 30).setScale(2, BigDecimal.ROUND_HALF_UP);
            case "SAL": // 盐度 30-40 PSU
                return BigDecimal.valueOf(30 + random.nextDouble() * 10).setScale(2, BigDecimal.ROUND_HALF_UP);
            case "CURRENT": // 流速 0-2 m/s
                return BigDecimal.valueOf(random.nextDouble() * 2).setScale(3, BigDecimal.ROUND_HALF_UP);
            case "WAVE": // 波高 0-10 m
                return BigDecimal.valueOf(random.nextDouble() * 10).setScale(2, BigDecimal.ROUND_HALF_UP);
            case "DO": // 溶解氧 0-10 mg/L
                return BigDecimal.valueOf(random.nextDouble() * 10).setScale(2, BigDecimal.ROUND_HALF_UP);
            case "CHL": // 叶绿素 0-50 μg/L
                return BigDecimal.valueOf(random.nextDouble() * 50).setScale(2, BigDecimal.ROUND_HALF_UP);
            default:
                return BigDecimal.valueOf(random.nextDouble() * 100).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * 生成可视化场景
     */
    private List<VisualizationScene> generateVisualizationScenes() {
        List<VisualizationScene> list = new ArrayList<>();
        String[] names = {"3D海洋温度场", "太平洋海流可视化", "全球海浪分布", "南海观测场景", "大西洋数据展示"};
        SceneTypeEnum[] types = {SceneTypeEnum.OCEAN_3D, SceneTypeEnum.CHART_2D, SceneTypeEnum.COMPOSITE, 
                                 SceneTypeEnum.OCEAN_3D, SceneTypeEnum.CHART_2D};
        PublicFlagEnum[] publicFlags = {PublicFlagEnum.YES, PublicFlagEnum.NO, PublicFlagEnum.YES, 
                                       PublicFlagEnum.NO, PublicFlagEnum.YES};

        for (int i = 0; i < names.length; i++) {
            VisualizationScene scene = new VisualizationScene();
            scene.setSceneName(names[i]);
            scene.setSceneType(types[i]);
            scene.setUserId(1L);
            scene.setIsPublic(publicFlags[i]);
            scene.setConfigJson("{\"view\":{\"center\":[120,25],\"zoom\":5}}");
            scene.setThumbnail("/images/scene_" + (i + 1) + ".jpg");
            scene.setDescription("这是" + names[i] + "的场景描述");
            scene.setViewCount(random.nextInt(1000));
            list.add(scene);
        }
        return list;
    }

    /**
     * 生成图表配置
     */
    private List<ChartConfig> generateChartConfigs() {
        List<ChartConfig> list = new ArrayList<>();
        String[] names = {"温度趋势图", "盐度分布柱状图", "海流速度散点图", "海浪高度热力图", "溶解氧3D表面图"};
        ChartTypeEnum[] types = {ChartTypeEnum.LINE, ChartTypeEnum.BAR, ChartTypeEnum.SCATTER, 
                                ChartTypeEnum.HEATMAP, ChartTypeEnum.SURFACE_3D};
        PublicFlagEnum[] publicFlags = {PublicFlagEnum.YES, PublicFlagEnum.YES, PublicFlagEnum.NO, 
                                       PublicFlagEnum.YES, PublicFlagEnum.NO};

        for (int i = 0; i < names.length; i++) {
            ChartConfig config = new ChartConfig();
            config.setChartName(names[i]);
            config.setChartType(types[i]);
            config.setUserId(1L);
            config.setDataQueryConfig("{\"dataSourceId\":1,\"timeRange\":\"30d\"}");
            config.setEchartsConfig("{\"title\":{\"text\":\"" + names[i] + "\"},\"xAxis\":{},\"yAxis\":{}}");
            config.setIsPublic(publicFlags[i]);
            config.setDescription("这是" + names[i] + "的图表配置");
            list.add(config);
        }
        return list;
    }

    /**
     * 生成系统配置
     */
    private List<SystemConfig> generateSystemConfigs() {
        List<SystemConfig> list = new ArrayList<>();
        String[] keys = {"system.name", "system.version", "api.timeout", "data.sync.interval", "max.upload.size"};
        String[] values = {"海洋数据可视化系统", "1.0.0", "30", "3600", "104857600"};
        ConfigTypeEnum[] types = {ConfigTypeEnum.STRING, ConfigTypeEnum.STRING, ConfigTypeEnum.NUMBER, 
                                 ConfigTypeEnum.NUMBER, ConfigTypeEnum.NUMBER};
        String[] groups = {"system", "system", "api", "data", "upload"};
        Boolean[] isSystem = {true, true, true, false, false};

        for (int i = 0; i < keys.length; i++) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(keys[i]);
            config.setConfigValue(values[i]);
            config.setConfigType(types[i]);
            config.setConfigGroup(groups[i]);
            config.setIsSystem(isSystem[i]);
            config.setDescription(keys[i] + "的配置说明");
            list.add(config);
        }
        return list;
    }
}


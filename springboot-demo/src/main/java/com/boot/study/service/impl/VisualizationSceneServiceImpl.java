package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.api.req.admin.ObservationDataPageReq;
import com.boot.study.api.req.admin.VisualizationScenePageReq;
import com.boot.study.api.req.user.SceneDataQueryReq;
import com.boot.study.api.req.user.UserScenePageReq;
import com.boot.study.dao.VisualizationSceneMapper;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.VisualizationScene;
import com.boot.study.entity.VisualizationSceneVO;
import com.boot.study.enums.PublicFlagEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.response.PageBean;
import com.boot.study.service.ObservationDataService;
import com.boot.study.service.ObservationDataTypeService;
import com.boot.study.service.DataSourceService;
import com.boot.study.service.VisualizationSceneService;
import com.boot.study.utils.DateTimeUtil;
import com.boot.study.utils.EnumConvertUtil;
import com.boot.study.utils.PageHelperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 可视化场景表 Service 实现
 *
 * @author study
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisualizationSceneServiceImpl extends BaseServiceImpl<VisualizationSceneMapper, VisualizationScene> implements VisualizationSceneService {

    private final ObservationDataService observationDataService;
    private final ObservationDataTypeService observationDataTypeService;
    private final DataSourceService dataSourceService;

    @Override
    public PageBean<VisualizationScene> pageList(VisualizationScenePageReq req) {
        LambdaQueryWrapper<VisualizationScene> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(req.getSceneName())) {
            wrapper.like(VisualizationScene::getSceneName, req.getSceneName());
        }
        if (req.getSceneType() != null) {
            wrapper.eq(VisualizationScene::getSceneType, req.getSceneType());
        }
        if (req.getIsPublic() != null) {
            wrapper.eq(VisualizationScene::getIsPublic, req.getIsPublic());
        }
        wrapper.orderByDesc(VisualizationScene::getCreatedTime);

        UserScenePageReq advancedReq = toUserScenePageReq(req);
        if (!hasAdvancedSceneFilter(advancedReq)) {
            Page<VisualizationScene> page = this.page(PageHelperUtil.page(req), wrapper);
            return PageBean.page(page, page.getRecords());
        }

        List<VisualizationScene> all = this.list(wrapper);
        List<VisualizationScene> filtered = applyAdvancedSceneFilters(all, advancedReq);
        return paginateSceneList(filtered, req.getPageNum(), req.getPageSize());
    }

    @Override
    public PageBean<VisualizationSceneVO> pageListWithVO(VisualizationScenePageReq req) {
        PageBean<VisualizationScene> pageBean = pageList(req);
        List<VisualizationSceneVO> voList = pageBean.getList().stream()
                .map(EnumConvertUtil::convertVisualizationScene)
                .collect(Collectors.toList());

        PageBean<VisualizationSceneVO> voPageBean = new PageBean<>();
        voPageBean.setPageNum(pageBean.getPageNum());
        voPageBean.setPageSize(pageBean.getPageSize());
        voPageBean.setSize(pageBean.getSize());
        voPageBean.setTotal(pageBean.getTotal());
        voPageBean.setPages(pageBean.getPages());
        voPageBean.setList(voList);
        return voPageBean;
    }

    /**
     * 用户端 - 分页查询公开的场景配置
     * <p>
     * 只查询公开且未删除的场景配置，支持按场景名称、场景类型筛选
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果（VO对象，包含枚举描述字段）
     */
    @Override
    public PageBean<VisualizationSceneVO> getPublicScenes(UserScenePageReq req) {
        LambdaQueryWrapper<VisualizationScene> wrapper = Wrappers.lambdaQuery();
        // 只查询公开场景
        wrapper.eq(VisualizationScene::getIsPublic, PublicFlagEnum.YES);
        wrapper.eq(VisualizationScene::getDeletedFlag, false); // 未删除

        // 场景名称模糊查询
        if (StringUtils.hasText(req.getSceneName())) {
            wrapper.like(VisualizationScene::getSceneName, req.getSceneName().trim());
        }
        // 场景类型精确查询
        if (req.getSceneType() != null) {
            wrapper.eq(VisualizationScene::getSceneType, req.getSceneType());
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(VisualizationScene::getCreatedTime);

        PageBean<VisualizationScene> sourcePage;
        if (!hasAdvancedSceneFilter(req)) {
            Page<VisualizationScene> page = this.page(PageHelperUtil.page(req), wrapper);
            sourcePage = PageBean.page(page, page.getRecords());
        } else {
            List<VisualizationScene> all = this.list(wrapper);
            List<VisualizationScene> filtered = applyAdvancedSceneFilters(all, req);
            sourcePage = paginateSceneList(filtered, req.getPageNum(), req.getPageSize());
        }

        // 转换为VO
        List<VisualizationSceneVO> voList = sourcePage.getList().stream()
                .map(EnumConvertUtil::convertVisualizationScene)
                .collect(Collectors.toList());

        // 构建VO分页结果
        PageBean<VisualizationSceneVO> pageBean = new PageBean<>();
        pageBean.setList(voList);
        pageBean.setTotal(sourcePage.getTotal());
        pageBean.setPageNum(sourcePage.getPageNum());
        pageBean.setPageSize(sourcePage.getPageSize());
        pageBean.setSize(voList.size());
        pageBean.setPages(sourcePage.getPages());

        return pageBean;
    }

    private boolean hasAdvancedSceneFilter(UserScenePageReq req) {
        return StringUtils.hasText(req.getStationKeyword())
                || StringUtils.hasText(req.getStationType())
                || StringUtils.hasText(req.getOceanRegion())
                || StringUtils.hasText(req.getProgramOwner())
                || StringUtils.hasText(req.getDataTypeCode())
                || req.getMinLongitude() != null
                || req.getMaxLongitude() != null
                || req.getMinLatitude() != null
                || req.getMaxLatitude() != null;
    }

    private UserScenePageReq toUserScenePageReq(VisualizationScenePageReq req) {
        UserScenePageReq advanced = new UserScenePageReq();
        advanced.setPageNum(req.getPageNum());
        advanced.setPageSize(req.getPageSize());
        advanced.setStationKeyword(req.getStationKeyword());
        advanced.setStationType(req.getStationType());
        advanced.setOceanRegion(req.getOceanRegion());
        advanced.setDataTypeCode(req.getDataTypeCode());
        advanced.setMinLongitude(req.getMinLongitude());
        advanced.setMaxLongitude(req.getMaxLongitude());
        advanced.setMinLatitude(req.getMinLatitude());
        advanced.setMaxLatitude(req.getMaxLatitude());
        return advanced;
    }

    private List<VisualizationScene> applyAdvancedSceneFilters(List<VisualizationScene> scenes, UserScenePageReq req) {
        if (scenes == null || scenes.isEmpty()) return Collections.emptyList();
        Map<Long, DataSource> dataSourceMap = dataSourceService.list().stream()
                .filter(ds -> ds.getId() != null)
                .collect(Collectors.toMap(DataSource::getId, ds -> ds, (a, b) -> a, HashMap::new));
        String keyword = lowerTrim(req.getStationKeyword());
        String stationType = normalizeCode(req.getStationType());
        String oceanRegion = normalizeCode(req.getOceanRegion());
        String programOwner = lowerTrim(req.getProgramOwner());
        String dataTypeCode = normalizeCode(req.getDataTypeCode());
        Map<Long, String> dataTypeIdToCode = Collections.emptyMap();
        if (StringUtils.hasText(dataTypeCode)) {
            dataTypeIdToCode = observationDataTypeService.list().stream()
                    .filter(t -> t.getId() != null && StringUtils.hasText(t.getTypeCode()))
                    .collect(Collectors.toMap(ObservationDataType::getId, t -> normalizeCode(t.getTypeCode()), (a, b) -> a, HashMap::new));
        }
        final Map<Long, String> finalDataTypeIdToCode = dataTypeIdToCode;

        return scenes.stream().filter(scene -> {
            SceneQueryMeta meta = parseSceneQueryMeta(scene.getConfigJson());
            DataSource ds = meta.dataSourceId == null ? null : dataSourceMap.get(meta.dataSourceId);
            if (StringUtils.hasText(keyword)) {
                boolean matched = false;
                if (ds != null) {
                    String sourceName = lowerTrim(ds.getSourceName());
                    String stationId = lowerTrim(ds.getStationId());
                    if (sourceName.contains(keyword) || stationId.contains(keyword)) matched = true;
                }
                // 兜底：扫描场景名 + configJson，覆盖未链接 dataSourceId 但配置中提及站点的场景
                if (!matched) {
                    String sceneName = lowerTrim(scene.getSceneName());
                    if (sceneName.contains(keyword)) matched = true;
                }
                if (!matched && StringUtils.hasText(scene.getConfigJson())) {
                    if (scene.getConfigJson().toLowerCase().contains(keyword)) matched = true;
                }
                if (!matched) return false;
            }
            if (StringUtils.hasText(stationType)) {
                if (ds == null || !stationType.equals(stationTypeCode(ds))) return false;
            }
            if (StringUtils.hasText(oceanRegion)) {
                if (ds == null || !oceanRegion.equals(resolveOceanRegion(ds.getLatitude(), ds.getLongitude()))) return false;
            }
            if (StringUtils.hasText(programOwner)) {
                if (ds == null || !resolveProgramOwner(ds).contains(programOwner)) return false;
            }
            if (req.getMinLongitude() != null || req.getMaxLongitude() != null
                    || req.getMinLatitude() != null || req.getMaxLatitude() != null) {
                if (ds == null || ds.getLongitude() == null || ds.getLatitude() == null) return false;
                if (!inRange(ds.getLongitude(), req.getMinLongitude(), req.getMaxLongitude())
                        || !inRange(ds.getLatitude(), req.getMinLatitude(), req.getMaxLatitude())) {
                    return false;
                }
            }
            if (StringUtils.hasText(dataTypeCode)) {
                boolean hit = false;
                if (meta.typeCodes != null) {
                    hit = meta.typeCodes.stream().anyMatch(c -> normalizeCode(c).equals(dataTypeCode));
                }
                if (!hit && meta.dataTypeId != null) {
                    String code = finalDataTypeIdToCode.get(meta.dataTypeId);
                    hit = StringUtils.hasText(code) && code.equals(dataTypeCode);
                }
                if (!hit && StringUtils.hasText(scene.getConfigJson())) {
                    hit = scene.getConfigJson().toUpperCase().contains(dataTypeCode);
                }
                if (!hit) return false;
            }
            return true;
        }).collect(Collectors.toList());
    }

    private static final class SceneQueryMeta {
        private Long dataSourceId;
        private Long dataTypeId;
        private List<String> typeCodes = Collections.emptyList();
    }

    private SceneQueryMeta parseSceneQueryMeta(String configJson) {
        SceneQueryMeta meta = new SceneQueryMeta();
        if (!StringUtils.hasText(configJson)) return meta;
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(configJson);
            if (json == null) return meta;
            Long direct = json.getLong("dataSourceId");
            if (direct != null) meta.dataSourceId = direct;
            Long auto = json.getLong("autoGenSourceId");
            if (meta.dataSourceId == null && auto != null) meta.dataSourceId = auto;
            meta.dataTypeId = json.getLong("dataTypeId");
            com.alibaba.fastjson2.JSONObject dataQuery = json.getJSONObject("dataQuery");
            List<String> typeCodes = new ArrayList<>();
            if (dataQuery != null) {
                Long nested = dataQuery.getLong("dataSourceId");
                if (meta.dataSourceId == null && nested != null) meta.dataSourceId = nested;
                Long nestedAuto = dataQuery.getLong("autoGenSourceId");
                if (meta.dataSourceId == null && nestedAuto != null) meta.dataSourceId = nestedAuto;
                Long nestedTypeId = dataQuery.getLong("dataTypeId");
                if (meta.dataTypeId == null && nestedTypeId != null) meta.dataTypeId = nestedTypeId;
                addTypeCodes(typeCodes, dataQuery.getList("typeCodes", String.class));
            }
            addTypeCodes(typeCodes, json.getList("typeCodes", String.class));
            List<com.alibaba.fastjson2.JSONObject> layers = json.getList("layers", com.alibaba.fastjson2.JSONObject.class);
            if (layers != null) {
                for (com.alibaba.fastjson2.JSONObject layer : layers) {
                    com.alibaba.fastjson2.JSONObject bindings = layer.getJSONObject("dataBindings");
                    if (bindings == null) continue;
                    for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                        Object v = entry.getValue();
                        if (v instanceof String && StringUtils.hasText((String) v)) {
                            typeCodes.add((String) v);
                        }
                    }
                }
            }
            meta.typeCodes = typeCodes;
            return meta;
        } catch (Exception e) {
            log.debug("解析场景 dataSourceId 失败，config={}", configJson, e);
            return meta;
        }
    }

    private void addTypeCodes(List<String> target, List<String> source) {
        if (target == null || source == null) return;
        for (String code : source) {
            if (StringUtils.hasText(code)) target.add(code);
        }
    }

    private String stationTypeCode(DataSource ds) {
        String suffix = ds.getFileSuffixes() == null ? "" : ds.getFileSuffixes().toLowerCase();
        String config = ds.getConfigJson() == null ? "" : ds.getConfigJson().toLowerCase();
        if (suffix.contains("dart") || config.contains("\"dart\":true") || config.contains("\"dart\":\"y\"")) return "DART";
        if (suffix.contains("adcp") || config.contains("\"currents\":true") || config.contains("\"currents\":\"y\"")) return "CURRENT";
        if (suffix.contains("tide") || suffix.contains("wlevel")) return "WATER_LEVEL";
        if (suffix.contains("spec") || suffix.contains("data_spec") || suffix.contains("swden")) return "WAVE";
        if (suffix.contains("ocean") || config.contains("\"waterquality\":true") || config.contains("\"waterquality\":\"y\"")) return "OCEAN";
        if (suffix.contains("txt") || suffix.contains("cwind") || suffix.contains("rain") || suffix.contains("srad")
                || config.contains("\"met\":true") || config.contains("\"met\":\"y\"")) return "MET";
        return normalizeCode(ds.getSourceType());
    }

    private String resolveOceanRegion(BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) return "";
        double la = lat.doubleValue();
        double lo = lon.doubleValue();
        while (lo > 180) lo -= 360;
        while (lo <= -180) lo += 360;
        if (la >= 66.5) return "ARCTIC";
        if (la <= -60) return "SOUTHERN";
        if (la >= 18 && la <= 31 && lo >= -98 && lo <= -80) return "GULF_OF_MEXICO";
        if (la >= 41 && la <= 49 && lo >= -93 && lo <= -76) return "GREAT_LAKES";
        if (la >= -60 && la <= 70 && lo >= -70 && lo <= 20) return "ATLANTIC";
        if (la >= -60 && la <= 30 && lo >= 20 && lo <= 100) return "INDIAN";
        return "PACIFIC";
    }

    private String resolveProgramOwner(DataSource ds) {
        if (ds == null) return "";
        String desc = StringUtils.hasText(ds.getDescription()) ? ds.getDescription() : "";
        String config = ds.getConfigJson();
        if (StringUtils.hasText(config)) {
            try {
                com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(config);
                if (json != null) {
                    String owner = json.getString("owner");
                    String program = json.getString("program");
                    if (StringUtils.hasText(owner)) return lowerTrim(owner);
                    if (StringUtils.hasText(program)) return lowerTrim(program);
                }
            } catch (Exception ignored) {
                // ignore invalid config json
            }
        }
        if ("NOAA".equalsIgnoreCase(ds.getSourceType())) return "noaa ndbc";
        return lowerTrim(desc);
    }

    private boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return false;
        if (min != null && value.compareTo(min) < 0) return false;
        if (max != null && value.compareTo(max) > 0) return false;
        return true;
    }

    private String normalizeCode(String raw) {
        return StringUtils.hasText(raw) ? raw.trim().toUpperCase() : "";
    }

    private String lowerTrim(String raw) {
        return StringUtils.hasText(raw) ? raw.trim().toLowerCase() : "";
    }

    private PageBean<VisualizationScene> paginateSceneList(List<VisualizationScene> list, int pageNum, int pageSize) {
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.max(1, pageSize);
        int total = list == null ? 0 : list.size();
        int from = Math.min((safePageNum - 1) * safePageSize, total);
        int to = Math.min(from + safePageSize, total);
        List<VisualizationScene> pageList = total == 0 ? Collections.emptyList() : list.subList(from, to);

        PageBean<VisualizationScene> bean = new PageBean<>();
        bean.setPageNum(safePageNum);
        bean.setPageSize(safePageSize);
        bean.setTotal(total);
        bean.setPages((int) Math.ceil(total / (double) safePageSize));
        bean.setSize(pageList.size());
        bean.setList(pageList);
        return bean;
    }

    /**
     * 用户端 - 根据ID获取公开的场景详情
     * <p>
     * 验证场景是否存在、是否公开、是否已删除，并增加查看次数
     *
     * @param id 场景ID
     * @return 场景配置VO对象
     * @throws ServiceException 如果场景不存在或无权访问
     */
    @Override
    public VisualizationSceneVO getPublicSceneById(Long id) {
        VisualizationScene scene = this.getById(id);
        if (scene == null || Boolean.TRUE.equals(scene.getDeletedFlag())) {
            throw new ServiceException(404, "场景不存在");
        }
        if (scene.getIsPublic() != PublicFlagEnum.YES) {
            throw new ServiceException(403, "无权访问该场景");
        }

        // 增加查看次数
        if (scene.getViewCount() == null) {
            scene.setViewCount(0);
        }
        scene.setViewCount(scene.getViewCount() + 1);
        this.updateById(scene);

        return EnumConvertUtil.convertVisualizationScene(scene);
    }

    /**
     * 用户端 - 查询场景关联的观测数据
     * <p>
     * 验证场景是否存在且公开，然后查询关联的观测数据，支持经纬度范围过滤
     *
     * @param sceneId 场景ID
     * @param req 数据查询请求
     * @return 观测数据分页结果
     * @throws ServiceException 如果场景不存在或无权访问
     */
    @Override
    public PageBean<ObservationDataVO> getSceneData(Long sceneId, SceneDataQueryReq req) {
        // 验证场景是否存在且公开
        VisualizationScene scene = this.getById(sceneId);
        if (scene == null || Boolean.TRUE.equals(scene.getDeletedFlag())) {
            throw new ServiceException(404, "场景不存在");
        }
        if (scene.getIsPublic() != PublicFlagEnum.YES) {
            throw new ServiceException(403, "无权访问该场景");
        }

        // 设置场景ID
        req.setSceneId(sceneId);

        // 构建查询条件
        ObservationDataPageReq dataReq = new ObservationDataPageReq();
        dataReq.setPageNum(req.getPageNum());
        dataReq.setPageSize(req.getPageSize());

        // 1. 先从场景配置 configJson 中解析出需要的数据类型（dataBindings 里的 *TypeCode）
        if (StringUtils.hasText(scene.getConfigJson())) {
            try {
                com.alibaba.fastjson2.JSONObject config =
                        com.alibaba.fastjson2.JSON.parseObject(scene.getConfigJson());
                if (config != null) {
                    List<String> typeCodes = new ArrayList<>();
                    // 顶层 dataQuery 中可能配置了 dataSourceId
                    com.alibaba.fastjson2.JSONObject dataQuery = config.getJSONObject("dataQuery");
                    if (dataQuery != null) {
                        Long cfgDataSourceId = dataQuery.getLong("dataSourceId");
                        if (cfgDataSourceId != null) {
                            dataReq.setDataSourceId(cfgDataSourceId);
                        }
                    }

                    // layers[*].dataBindings 下的 *TypeCode，全部收集
                    List<com.alibaba.fastjson2.JSONObject> layers = config.getList("layers", com.alibaba.fastjson2.JSONObject.class);
                    if (layers != null) {
                        for (com.alibaba.fastjson2.JSONObject layer : layers) {
                            com.alibaba.fastjson2.JSONObject bindings = layer.getJSONObject("dataBindings");
                            if (bindings != null) {
                                for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                                    Object v = entry.getValue();
                                    if (v instanceof String && ((String) v).length() > 0) {
                                        typeCodes.add((String) v);
                                    }
                                }
                            }
                        }
                    }

                    if (!typeCodes.isEmpty()) {
                        List<ObservationDataType> types = observationDataTypeService.list(
                                Wrappers.<ObservationDataType>lambdaQuery()
                                        .in(ObservationDataType::getTypeCode, typeCodes)
                        );
                        if (!types.isEmpty()) {
                            List<Long> typeIds = types.stream()
                                    .map(ObservationDataType::getId)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                            dataReq.setDataTypeIds(typeIds);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("解析场景 configJson 失败, sceneId={}", sceneId, e);
            }
        }

        // 2. 再叠加前端传入的过滤条件（如 dataSourceId / dataTypeId / 时间范围等）
        if (req.getDataSourceId() != null) {
            dataReq.setDataSourceId(req.getDataSourceId());
        }
        if (req.getDataTypeId() != null) {
            dataReq.setDataTypeId(req.getDataTypeId());
            dataReq.setDataTypeIds(null);
        }
        if (req.getTypeCodes() != null && !req.getTypeCodes().isEmpty()) {
            // 若显式指定 typeCodes，则以其为准（覆盖场景 configJson 解析出来的类型集合）
            List<ObservationDataType> types = observationDataTypeService.list(
                    Wrappers.<ObservationDataType>lambdaQuery()
                            .in(ObservationDataType::getTypeCode, req.getTypeCodes())
            );
            if (!types.isEmpty()) {
                List<Long> typeIds = types.stream()
                        .map(ObservationDataType::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                dataReq.setDataTypeId(null);
                dataReq.setDataTypeIds(typeIds);
            }
        }
        dataReq.setStartTime(DateTimeUtil.parseToLocalDateTime(req.getStartTime()));
        dataReq.setEndTime(DateTimeUtil.parseToLocalDateTime(req.getEndTime()));
        // 经纬度范围过滤下推到数据库（避免分页后再过滤导致结果/total 不准确）
        dataReq.setMinLongitude(req.getMinLongitude());
        dataReq.setMaxLongitude(req.getMaxLongitude());
        dataReq.setMinLatitude(req.getMinLatitude());
        dataReq.setMaxLatitude(req.getMaxLatitude());

        // 查询数据
        PageBean<ObservationDataVO> pageBean = observationDataService.pageListWithRelation(dataReq);

        return pageBean;
    }
}



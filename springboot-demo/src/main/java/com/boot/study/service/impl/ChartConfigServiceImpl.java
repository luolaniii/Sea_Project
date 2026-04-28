package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.api.req.admin.ChartConfigPageReq;
import com.boot.study.api.req.admin.ObservationDataPageReq;
import com.boot.study.api.req.user.ChartDataQueryReq;
import com.boot.study.api.req.user.UserChartPageReq;
import com.boot.study.dao.ChartConfigMapper;
import com.boot.study.entity.ChartConfig;
import com.boot.study.entity.ChartConfigVO;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.enums.PublicFlagEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.response.PageBean;
import com.boot.study.service.ChartConfigService;
import com.boot.study.service.DataSourceService;
import com.boot.study.service.ObservationDataService;
import com.boot.study.service.ObservationDataTypeService;
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
 * 图表配置表 Service 实现类
 * <p>
 * 实现图表配置相关的业务逻辑，包括分页查询、数据转换等功能
 *
 * @author study
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChartConfigServiceImpl extends BaseServiceImpl<ChartConfigMapper, ChartConfig> implements ChartConfigService {

    private final ObservationDataService observationDataService;
    private final ObservationDataTypeService observationDataTypeService;
    private final DataSourceService dataSourceService;

    /**
     * 分页查询图表配置
     * <p>
     * 根据查询条件构建查询条件，支持按图表名称模糊查询、图表类型精确查询、
     * 是否公开精确查询，并按创建时间倒序排列
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果，包含图表配置列表
     */
    @Override
    public PageBean<ChartConfig> pageList(ChartConfigPageReq req) {
        LambdaQueryWrapper<ChartConfig> wrapper = Wrappers.lambdaQuery();

        // 图表名称模糊查询
        if (StringUtils.hasText(req.getChartName())) {
            wrapper.like(ChartConfig::getChartName, req.getChartName());
        }

        // 图表类型精确查询
        if (req.getChartType() != null) {
            wrapper.eq(ChartConfig::getChartType, req.getChartType());
        }

        // 是否公开精确查询
        if (req.getIsPublic() != null) {
            wrapper.eq(ChartConfig::getIsPublic, req.getIsPublic());
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(ChartConfig::getCreatedTime);

        // 仅名称/类型/公开状态筛选时走数据库分页；涉及站点/数据类型/经纬度时走内存过滤分页
        if (!hasAdvancedChartFilter(req)) {
            Page<ChartConfig> page = this.page(PageHelperUtil.page(req), wrapper);
            return PageBean.page(page, page.getRecords());
        }

        List<ChartConfig> all = this.list(wrapper);
        List<ChartConfig> filtered = applyAdvancedChartFilters(
                all,
                req.getStationKeyword(),
                req.getStationType(),
                req.getOceanRegion(),
                req.getProgramOwner(),
                req.getDataTypeCode(),
                req.getMinLongitude(),
                req.getMaxLongitude(),
                req.getMinLatitude(),
                req.getMaxLatitude()
        );
        return paginateList(filtered, req.getPageNum(), req.getPageSize());
    }

    /**
     * 分页查询图表配置（包含枚举描述）
     * <p>
     * 先调用分页查询方法获取数据，然后将实体对象转换为包含枚举描述的VO对象
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果（VO对象，包含枚举描述字段）
     */
    @Override
    public PageBean<ChartConfigVO> pageListWithVO(ChartConfigPageReq req) {
        // 先获取分页数据
        PageBean<ChartConfig> pageBean = pageList(req);
        
        // 将实体对象转换为VO对象（包含枚举描述）
        List<ChartConfigVO> voList = pageBean.getList().stream()
                .map(EnumConvertUtil::convertChartConfig)
                .collect(Collectors.toList());

        // 构建VO分页结果
        PageBean<ChartConfigVO> voPageBean = new PageBean<>();
        voPageBean.setPageNum(pageBean.getPageNum());
        voPageBean.setPageSize(pageBean.getPageSize());
        voPageBean.setSize(pageBean.getSize());
        voPageBean.setTotal(pageBean.getTotal());
        voPageBean.setPages(pageBean.getPages());
        voPageBean.setList(voList);
        return voPageBean;
    }

    /**
     * 用户端 - 分页查询公开的图表配置
     * <p>
     * 只查询公开且未删除的图表配置，支持按图表名称、图表类型筛选
     *
     * @param req 查询请求，包含分页参数和筛选条件
     * @return 分页结果（VO对象，包含枚举描述字段）
     */
    @Override
    public PageBean<ChartConfigVO> getPublicCharts(UserChartPageReq req) {
        LambdaQueryWrapper<ChartConfig> wrapper = Wrappers.lambdaQuery();
        // 只查询公开图表
        wrapper.eq(ChartConfig::getIsPublic, PublicFlagEnum.YES);
        wrapper.eq(ChartConfig::getDeletedFlag, false); // 未删除

        // 图表名称模糊查询
        if (StringUtils.hasText(req.getChartName())) {
            wrapper.like(ChartConfig::getChartName, req.getChartName().trim());
        }
        // 图表类型精确查询
        if (req.getChartType() != null) {
            wrapper.eq(ChartConfig::getChartType, req.getChartType());
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(ChartConfig::getCreatedTime);

        PageBean<ChartConfig> pageBean;
        if (!hasAdvancedChartFilter(req)) {
            // 分页查询
            Page<ChartConfig> page = this.page(PageHelperUtil.page(req), wrapper);
            pageBean = PageBean.page(page, page.getRecords());
        } else {
            List<ChartConfig> all = this.list(wrapper);
            List<ChartConfig> filtered = applyAdvancedChartFilters(
                    all,
                    req.getStationKeyword(),
                    req.getStationType(),
                    req.getOceanRegion(),
                    req.getProgramOwner(),
                    req.getDataTypeCode(),
                    req.getMinLongitude(),
                    req.getMaxLongitude(),
                    req.getMinLatitude(),
                    req.getMaxLatitude()
            );
            pageBean = paginateList(filtered, req.getPageNum(), req.getPageSize());
        }

        // 转换为VO
        List<ChartConfigVO> voList = pageBean.getList().stream()
                .map(EnumConvertUtil::convertChartConfig)
                .collect(Collectors.toList());

        // 构建VO分页结果
        PageBean<ChartConfigVO> voPageBean = new PageBean<>();
        voPageBean.setList(voList);
        voPageBean.setTotal(pageBean.getTotal());
        voPageBean.setPageNum(pageBean.getPageNum());
        voPageBean.setPageSize(pageBean.getPageSize());
        voPageBean.setSize(voList.size());
        voPageBean.setPages(pageBean.getPages());
        return voPageBean;
    }

    /**
     * 用户端 - 根据ID获取公开的图表详情
     * <p>
     * 验证图表是否存在、是否公开、是否已删除
     *
     * @param id 图表ID
     * @return 图表配置VO对象
     * @throws ServiceException 如果图表不存在或无权访问
     */
    @Override
    public ChartConfigVO getPublicChartById(Long id) {
        ChartConfig chart = this.getById(id);
        if (chart == null || Boolean.TRUE.equals(chart.getDeletedFlag())) {
            throw new ServiceException(404, "图表不存在");
        }
        if (chart.getIsPublic() != PublicFlagEnum.YES) {
            throw new ServiceException(403, "无权访问该图表");
        }

        return EnumConvertUtil.convertChartConfig(chart);
    }

    /**
     * 用户端 - 查询图表关联的观测数据
     * <p>
     * 验证图表是否存在且公开，然后查询关联的观测数据，支持经纬度范围过滤
     *
     * @param chartId 图表ID
     * @param req 数据查询请求
     * @return 观测数据分页结果
     * @throws ServiceException 如果图表不存在或无权访问
     */
    @Override
    public PageBean<ObservationDataVO> getChartData(Long chartId, ChartDataQueryReq req) {
        // 验证图表是否存在且公开
        ChartConfig chart = this.getById(chartId);
        if (chart == null || Boolean.TRUE.equals(chart.getDeletedFlag())) {
            throw new ServiceException(404, "图表不存在");
        }
        if (chart.getIsPublic() != PublicFlagEnum.YES) {
            throw new ServiceException(403, "无权访问该图表");
        }

        // 设置图表ID
        req.setChartId(chartId);

        // 构建查询条件（优先使用图表配置中的 dataQueryConfig，再叠加前端传入的过滤条件）
        ObservationDataPageReq dataReq = new ObservationDataPageReq();
        dataReq.setPageNum(req.getPageNum());
        dataReq.setPageSize(req.getPageSize());

        // 1. 先从图表配置中的 dataQueryConfig 解析出 dataSourceId、dataTypeId、typeCodes 等
        ChartQueryMeta configMeta = parseChartQueryMeta(chart.getDataQueryConfig());
        if (configMeta.dataSourceId != null) {
            dataReq.setDataSourceId(configMeta.dataSourceId);
        }
        if (configMeta.dataTypeId != null) {
            dataReq.setDataTypeId(configMeta.dataTypeId);
        }
        if (configMeta.typeCodes != null && !configMeta.typeCodes.isEmpty()) {
            List<ObservationDataType> types = observationDataTypeService.list(
                    Wrappers.<ObservationDataType>lambdaQuery()
                            .in(ObservationDataType::getTypeCode, configMeta.typeCodes)
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

        // 2. 再用前端请求中的过滤条件进行“补充/覆盖”（例如时间范围、经纬度范围等）
        if (req.getDataSourceId() != null) {
            dataReq.setDataSourceId(req.getDataSourceId());
        }
        if (req.getDataTypeId() != null) {
            // 若显式指定单个数据类型，则以该类型为准
            dataReq.setDataTypeId(req.getDataTypeId());
            dataReq.setDataTypeIds(null);
        }
        if (req.getTypeCodes() != null && !req.getTypeCodes().isEmpty()) {
            // 若显式指定 typeCodes，则以其为准（覆盖 dataQueryConfig 解析出来的类型集合）
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

    private static final class ChartQueryMeta {
        private Long dataSourceId;
        private Long dataTypeId;
        private List<String> typeCodes;
    }

    private boolean hasAdvancedChartFilter(ChartConfigPageReq req) {
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

    private boolean hasAdvancedChartFilter(UserChartPageReq req) {
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

    private List<ChartConfig> applyAdvancedChartFilters(
            List<ChartConfig> source,
            String stationKeyword,
            String stationType,
            String oceanRegion,
            String programOwner,
            String dataTypeCode,
            BigDecimal minLongitude,
            BigDecimal maxLongitude,
            BigDecimal minLatitude,
            BigDecimal maxLatitude
    ) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        boolean needDataSource = StringUtils.hasText(stationKeyword)
                || StringUtils.hasText(stationType)
                || StringUtils.hasText(oceanRegion)
                || StringUtils.hasText(programOwner)
                || minLongitude != null
                || maxLongitude != null
                || minLatitude != null
                || maxLatitude != null;
        Map<Long, DataSource> dataSourceMap = Collections.emptyMap();
        if (needDataSource) {
            dataSourceMap = dataSourceService.list().stream()
                    .filter(ds -> ds.getId() != null)
                    .collect(Collectors.toMap(DataSource::getId, ds -> ds, (a, b) -> a, HashMap::new));
        }

        String dataTypeCodeNorm = normalizeCode(dataTypeCode);
        Map<Long, String> dataTypeIdToCode = Collections.emptyMap();
        if (StringUtils.hasText(dataTypeCodeNorm)) {
            dataTypeIdToCode = observationDataTypeService.list().stream()
                    .filter(t -> t.getId() != null && StringUtils.hasText(t.getTypeCode()))
                    .collect(Collectors.toMap(ObservationDataType::getId, t -> normalizeCode(t.getTypeCode()), (a, b) -> a, HashMap::new));
        }

        final String stationKeywordNorm = lowerTrim(stationKeyword);
        final String stationTypeNorm = normalizeCode(stationType);
        final String oceanRegionNorm = normalizeCode(oceanRegion);
        final String programOwnerNorm = lowerTrim(programOwner);
        final Map<Long, DataSource> finalDataSourceMap = dataSourceMap;
        final Map<Long, String> finalDataTypeIdToCode = dataTypeIdToCode;

        return source.stream().filter(chart -> {
            ChartQueryMeta meta = parseChartQueryMeta(chart.getDataQueryConfig());
            DataSource ds = meta.dataSourceId != null ? finalDataSourceMap.get(meta.dataSourceId) : null;

            // 站点关键词
            if (StringUtils.hasText(stationKeywordNorm)) {
                boolean matched = false;
                if (ds != null) {
                    String sourceName = lowerTrim(ds.getSourceName());
                    String stationId = lowerTrim(ds.getStationId());
                    if (sourceName.contains(stationKeywordNorm) || stationId.contains(stationKeywordNorm)) {
                        matched = true;
                    }
                }
                // 兜底：扫描图表名 + dataQueryConfig，覆盖未链接 dataSourceId 但配置/标题包含站点的图表
                if (!matched) {
                    String chartName = lowerTrim(chart.getChartName());
                    if (chartName.contains(stationKeywordNorm)) matched = true;
                }
                if (!matched && StringUtils.hasText(chart.getDataQueryConfig())) {
                    if (chart.getDataQueryConfig().toLowerCase().contains(stationKeywordNorm)) {
                        matched = true;
                    }
                }
                if (!matched) {
                    return false;
                }
            }

            // 站点类型（sourceType）
            if (StringUtils.hasText(stationTypeNorm)) {
                if (ds == null || !stationTypeCode(ds).equals(stationTypeNorm)) {
                    return false;
                }
            }

            // 海域分类（按站点经纬度推断）
            if (StringUtils.hasText(oceanRegionNorm)) {
                if (ds == null || !resolveOceanRegion(ds.getLatitude(), ds.getLongitude()).equals(oceanRegionNorm)) {
                    return false;
                }
            }

            // 归属/项目/Owner 关键词
            if (StringUtils.hasText(programOwnerNorm)) {
                if (ds == null || !resolveProgramOwner(ds).contains(programOwnerNorm)) {
                    return false;
                }
            }

            // 经纬度范围（按数据源坐标）
            if (minLongitude != null || maxLongitude != null || minLatitude != null || maxLatitude != null) {
                if (ds == null || ds.getLongitude() == null || ds.getLatitude() == null) {
                    return false;
                }
                if (!inRange(ds.getLongitude(), minLongitude, maxLongitude) || !inRange(ds.getLatitude(), minLatitude, maxLatitude)) {
                    return false;
                }
            }

            // 数据类型编码（typeCodes 或 dataTypeId 映射）
            if (StringUtils.hasText(dataTypeCodeNorm)) {
                boolean hit = false;
                if (meta.typeCodes != null) {
                    hit = meta.typeCodes.stream().anyMatch(c -> normalizeCode(c).equals(dataTypeCodeNorm));
                }
                if (!hit && meta.dataTypeId != null) {
                    String code = finalDataTypeIdToCode.get(meta.dataTypeId);
                    if (StringUtils.hasText(code)) {
                        hit = code.equals(dataTypeCodeNorm);
                    }
                }
                if (!hit) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());
    }

    private ChartQueryMeta parseChartQueryMeta(String dataQueryConfig) {
        ChartQueryMeta meta = new ChartQueryMeta();
        meta.typeCodes = Collections.emptyList();
        if (!StringUtils.hasText(dataQueryConfig)) {
            return meta;
        }
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(dataQueryConfig);
            if (json == null) return meta;
            com.alibaba.fastjson2.JSONObject dataQuery = json.getJSONObject("dataQuery");
            meta.dataSourceId = firstLong(
                    json.getLong("dataSourceId"),
                    json.getLong("autoGenSourceId"),
                    dataQuery == null ? null : dataQuery.getLong("dataSourceId"),
                    dataQuery == null ? null : dataQuery.getLong("autoGenSourceId")
            );
            meta.dataTypeId = firstLong(
                    json.getLong("dataTypeId"),
                    dataQuery == null ? null : dataQuery.getLong("dataTypeId")
            );

            List<String> typeCodes = new ArrayList<>();
            addTypeCodes(typeCodes, json.getList("typeCodes", String.class));
            if (dataQuery != null) {
                addTypeCodes(typeCodes, dataQuery.getList("typeCodes", String.class));
            }
            if (typeCodes != null && !typeCodes.isEmpty()) {
                meta.typeCodes = typeCodes;
            }
        } catch (Exception e) {
            log.debug("解析图表 dataQueryConfig 失败，忽略高级筛选条件。config={}", dataQueryConfig, e);
        }
        return meta;
    }

    private Long firstLong(Long... values) {
        if (values == null) return null;
        for (Long value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private void addTypeCodes(List<String> target, List<String> source) {
        if (target == null || source == null) return;
        for (String code : source) {
            if (StringUtils.hasText(code)) {
                target.add(code);
            }
        }
    }

    private String normalizeCode(String raw) {
        if (!StringUtils.hasText(raw)) return "";
        return raw.trim().toUpperCase();
    }

    private String lowerTrim(String raw) {
        if (!StringUtils.hasText(raw)) return "";
        return raw.trim().toLowerCase();
    }

    private boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return false;
        if (min != null && value.compareTo(min) < 0) return false;
        if (max != null && value.compareTo(max) > 0) return false;
        return true;
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

    private PageBean<ChartConfig> paginateList(List<ChartConfig> list, int pageNum, int pageSize) {
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = Math.max(1, pageSize);
        int total = list == null ? 0 : list.size();
        int from = Math.min((safePageNum - 1) * safePageSize, total);
        int to = Math.min(from + safePageSize, total);
        List<ChartConfig> pageList = total == 0 ? Collections.emptyList() : list.subList(from, to);

        PageBean<ChartConfig> bean = new PageBean<>();
        bean.setPageNum(safePageNum);
        bean.setPageSize(safePageSize);
        bean.setTotal(total);
        bean.setPages((int) Math.ceil(total / (double) safePageSize));
        bean.setSize(pageList.size());
        bean.setList(pageList);
        return bean;
    }
}

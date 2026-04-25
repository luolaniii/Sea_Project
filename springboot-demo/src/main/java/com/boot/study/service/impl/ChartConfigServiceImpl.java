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
import com.boot.study.entity.ObservationDataType;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.enums.PublicFlagEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.response.PageBean;
import com.boot.study.service.ChartConfigService;
import com.boot.study.service.ObservationDataService;
import com.boot.study.service.ObservationDataTypeService;
import com.boot.study.utils.DateTimeUtil;
import com.boot.study.utils.EnumConvertUtil;
import com.boot.study.utils.PageHelperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
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

        Page<ChartConfig> page = this.page(PageHelperUtil.page(req), wrapper);
        return PageBean.page(page, page.getRecords());
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

        // 分页查询
        Page<ChartConfig> page = this.page(PageHelperUtil.page(req), wrapper);

        // 转换为VO
        List<ChartConfigVO> voList = page.getRecords().stream()
                .map(EnumConvertUtil::convertChartConfig)
                .collect(Collectors.toList());

        // 构建VO分页结果
        PageBean<ChartConfigVO> pageBean = new PageBean<>();
        pageBean.setList(voList);
        pageBean.setTotal((int) page.getTotal());
        pageBean.setPageNum(req.getPageNum());
        pageBean.setPageSize(req.getPageSize());
        pageBean.setSize(page.getRecords().size());
        pageBean.setPages((int) page.getPages());

        return pageBean;
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

        // 1. 先从图表配置中的 dataQueryConfig 解析出 dataSourceId、typeCodes 等
        if (StringUtils.hasText(chart.getDataQueryConfig())) {
            try {
                com.alibaba.fastjson2.JSONObject config =
                        com.alibaba.fastjson2.JSON.parseObject(chart.getDataQueryConfig());
                if (config != null) {
                    Long cfgDataSourceId = config.getLong("dataSourceId");
                    if (cfgDataSourceId != null) {
                        dataReq.setDataSourceId(cfgDataSourceId);
                    }

                    // 解析 typeCodes（例如 ["WIND_SPEED","PRESSURE"]），转换为数据类型ID集合
                    List<String> typeCodes = config.getList("typeCodes", String.class);
                    if (typeCodes != null && !typeCodes.isEmpty()) {
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
                log.error("解析图表 dataQueryConfig 失败, chartId={}", chartId, e);
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
}



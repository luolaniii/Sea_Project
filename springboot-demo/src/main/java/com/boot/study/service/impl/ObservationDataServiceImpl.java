package com.boot.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boot.study.api.req.admin.ObservationDataPageReq;
import com.boot.study.dao.ObservationDataMapper;
import com.boot.study.entity.DataSource;
import com.boot.study.entity.ObservationData;
import com.boot.study.entity.ObservationDataType;
import com.boot.study.entity.ObservationDataVO;
import com.boot.study.enums.DataSourceTypeEnum;
import com.boot.study.response.PageBean;
import com.boot.study.service.DataSourceService;
import com.boot.study.service.ObservationDataService;
import com.boot.study.service.ObservationDataTypeService;
import com.boot.study.utils.EnumConvertUtil;
import com.boot.study.utils.PageHelperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 观测数据表 Service 实现
 *
 * @author study
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationDataServiceImpl extends BaseServiceImpl<ObservationDataMapper, ObservationData> implements ObservationDataService {

    private final DataSourceService dataSourceService;
    private final ObservationDataTypeService observationDataTypeService;

    @Override
    public PageBean<ObservationData> pageList(ObservationDataPageReq req) {
        LambdaQueryWrapper<ObservationData> wrapper = Wrappers.lambdaQuery();
        if (req.getDataSourceId() != null) {
            wrapper.eq(ObservationData::getDataSourceId, req.getDataSourceId());
        }
        // 支持单个或多个数据类型ID查询
        if (req.getDataTypeId() != null) {
            wrapper.eq(ObservationData::getDataTypeId, req.getDataTypeId());
        } else if (req.getDataTypeIds() != null && !req.getDataTypeIds().isEmpty()) {
            wrapper.in(ObservationData::getDataTypeId, req.getDataTypeIds());
        }
        if (StringUtils.hasText(req.getQualityFlag())) {
            wrapper.eq(ObservationData::getQualityFlag, req.getQualityFlag());
        }
        if (Objects.nonNull(req.getStartTime())) {
            wrapper.ge(ObservationData::getObservationTime, req.getStartTime());
        }
        if (Objects.nonNull(req.getEndTime())) {
            wrapper.le(ObservationData::getObservationTime, req.getEndTime());
        }
        // 经纬度范围过滤（用于图表/场景按空间范围查询；将过滤下推到数据库，避免“分页后再过滤”导致结果/total 不准确）
        if (req.getMinLongitude() != null) {
            wrapper.ge(ObservationData::getLongitude, BigDecimal.valueOf(req.getMinLongitude()));
        }
        if (req.getMaxLongitude() != null) {
            wrapper.le(ObservationData::getLongitude, BigDecimal.valueOf(req.getMaxLongitude()));
        }
        if (req.getMinLatitude() != null) {
            wrapper.ge(ObservationData::getLatitude, BigDecimal.valueOf(req.getMinLatitude()));
        }
        if (req.getMaxLatitude() != null) {
            wrapper.le(ObservationData::getLatitude, BigDecimal.valueOf(req.getMaxLatitude()));
        }
        wrapper.orderByDesc(ObservationData::getObservationTime);

        Page<ObservationData> page = this.page(PageHelperUtil.page(req), wrapper);
        return PageBean.page(page, page.getRecords());
    }

    @Override
    public PageBean<ObservationDataVO> pageListWithRelation(ObservationDataPageReq req) {
        // 先查询分页数据
        PageBean<ObservationData> pageBean = pageList(req);
        
        // 获取所有需要查询的数据源ID和数据类型ID
        List<ObservationData> dataList = pageBean.getList();
        List<Long> dataSourceIds = dataList.stream()
                .map(ObservationData::getDataSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> dataTypeIds = dataList.stream()
                .map(ObservationData::getDataTypeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询数据源和数据类型
        Map<Long, DataSource> dataSourceMap = dataSourceIds.isEmpty() ? 
                new java.util.HashMap<>() :
                dataSourceService.listByIds(dataSourceIds).stream()
                        .collect(Collectors.toMap(DataSource::getId, ds -> ds));
        Map<Long, ObservationDataType> dataTypeMap = dataTypeIds.isEmpty() ? 
                new java.util.HashMap<>() :
                observationDataTypeService.listByIds(dataTypeIds).stream()
                        .collect(Collectors.toMap(ObservationDataType::getId, dt -> dt));
        
        // 转换VO，填充数据源和数据类型信息
        List<ObservationDataVO> voList = dataList.stream().map(data -> {
            ObservationDataVO vo = EnumConvertUtil.convertObservationData(data);
            
            // 填充数据源信息
            if (data.getDataSourceId() != null) {
                DataSource dataSource = dataSourceMap.get(data.getDataSourceId());
                if (dataSource != null) {
                    vo.setDataSourceName(dataSource.getSourceName());
                    if (dataSource.getSourceType() != null) {
                        DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(dataSource.getSourceType());
                        vo.setDataSourceTypeDesc(typeEnum != null ? typeEnum.getDesc() : dataSource.getSourceType());
                    }
                }
            }
            
            // 填充数据类型信息
            if (data.getDataTypeId() != null) {
                ObservationDataType dataType = dataTypeMap.get(data.getDataTypeId());
                if (dataType != null) {
                    vo.setDataTypeName(dataType.getTypeName());
                    vo.setDataTypeCode(dataType.getTypeCode());
                    vo.setDataTypeUnit(dataType.getUnit());
                }
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        // 构建返回结果
        PageBean<ObservationDataVO> voPageBean = new PageBean<>();
        voPageBean.setPageNum(pageBean.getPageNum());
        voPageBean.setPageSize(pageBean.getPageSize());
        voPageBean.setSize(pageBean.getSize());
        voPageBean.setTotal(pageBean.getTotal());
        voPageBean.setPages(pageBean.getPages());
        voPageBean.setList(voList);
        
        return voPageBean;
    }
}



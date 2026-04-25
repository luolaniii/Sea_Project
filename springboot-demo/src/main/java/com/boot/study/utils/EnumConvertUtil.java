package com.boot.study.utils;

import com.boot.study.entity.*;
import com.boot.study.enums.*;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 枚举转换工具类
 * 用于将实体中的枚举字段转换为包含desc的VO对象
 */
public class EnumConvertUtil {

    /**
     * 转换DataSource，添加枚举desc字段
     */
    public static DataSourceVO convertDataSource(DataSource source) {
        if (source == null) {
            return null;
        }
        DataSourceVO vo = new DataSourceVO();
        BeanUtils.copyProperties(source, vo);
        
        // 转换数据源类型
        if (source.getSourceType() != null) {
            DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(source.getSourceType());
            vo.setSourceTypeDesc(typeEnum != null ? typeEnum.getDesc() : source.getSourceType());
        }
        
        // 转换状态
        if (source.getStatus() != null) {
            StatusEnum statusEnum = StatusEnum.fromCode(source.getStatus());
            vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : String.valueOf(source.getStatus()));
        }
        
        return vo;
    }

    /**
     * 转换ObservationData，添加枚举desc字段
     */
    public static ObservationDataVO convertObservationData(ObservationData source) {
        if (source == null) {
            return null;
        }
        ObservationDataVO vo = new ObservationDataVO();
        BeanUtils.copyProperties(source, vo);
        
        // 转换质量标志
        if (source.getQualityFlag() != null) {
            QualityFlagEnum qualityEnum = QualityFlagEnum.fromCode(source.getQualityFlag());
            vo.setQualityFlagDesc(qualityEnum != null ? qualityEnum.getDesc() : source.getQualityFlag());
        }
        
        return vo;
    }

    /**
     * 转换VisualizationScene，添加枚举desc字段
     */
    public static VisualizationSceneVO convertVisualizationScene(VisualizationScene source) {
        if (source == null) {
            return null;
        }
        VisualizationSceneVO vo = new VisualizationSceneVO();
        BeanUtils.copyProperties(source, vo);
        
        // 转换场景类型
        if (source.getSceneType() != null) {
            vo.setSceneTypeDesc(source.getSceneType().getDesc());
            vo.setSceneTypeCode(source.getSceneType().getCode());
        }
        
        // 转换公开标志
        if (source.getIsPublic() != null) {
            vo.setIsPublicDesc(source.getIsPublic().getDesc());
            vo.setIsPublicCode(source.getIsPublic().getCode());
        }
        
        return vo;
    }

    /**
     * 转换ChartConfig，添加枚举desc字段
     */
    public static ChartConfigVO convertChartConfig(ChartConfig source) {
        if (source == null) {
            return null;
        }
        ChartConfigVO vo = new ChartConfigVO();
        BeanUtils.copyProperties(source, vo);
        
        // 转换图表类型
        if (source.getChartType() != null) {
            vo.setChartTypeDesc(source.getChartType().getDesc());
            vo.setChartTypeCode(source.getChartType().getCode());
        }
        
        // 转换公开标志
        if (source.getIsPublic() != null) {
            vo.setIsPublicDesc(source.getIsPublic().getDesc());
            vo.setIsPublicCode(source.getIsPublic().getCode());
        }
        
        return vo;
    }

    /**
     * 转换SystemConfig，添加枚举desc字段
     */
    public static SystemConfigVO convertSystemConfig(SystemConfig source) {
        if (source == null) {
            return null;
        }
        SystemConfigVO vo = new SystemConfigVO();
        BeanUtils.copyProperties(source, vo);
        
        // 转换配置类型
        if (source.getConfigType() != null) {
            vo.setConfigTypeDesc(source.getConfigType().getDesc());
            vo.setConfigTypeCode(source.getConfigType().getCode());
        }
        
        return vo;
    }

    /**
     * 批量转换DataSource
     */
    public static List<DataSourceVO> convertDataSourceList(List<DataSource> sources) {
        return sources.stream().map(EnumConvertUtil::convertDataSource).collect(Collectors.toList());
    }

    /**
     * 批量转换ObservationData
     */
    public static List<ObservationDataVO> convertObservationDataList(List<ObservationData> sources) {
        return sources.stream().map(EnumConvertUtil::convertObservationData).collect(Collectors.toList());
    }

    /**
     * 批量转换VisualizationScene
     */
    public static List<VisualizationSceneVO> convertVisualizationSceneList(List<VisualizationScene> sources) {
        return sources.stream().map(EnumConvertUtil::convertVisualizationScene).collect(Collectors.toList());
    }

    /**
     * 批量转换ChartConfig
     */
    public static List<ChartConfigVO> convertChartConfigList(List<ChartConfig> sources) {
        return sources.stream().map(EnumConvertUtil::convertChartConfig).collect(Collectors.toList());
    }

    /**
     * 批量转换SystemConfig
     */
    public static List<SystemConfigVO> convertSystemConfigList(List<SystemConfig> sources) {
        return sources.stream().map(EnumConvertUtil::convertSystemConfig).collect(Collectors.toList());
    }
}


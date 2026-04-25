package com.boot.study.service.impl;

import com.boot.study.enums.*;
import com.boot.study.exception.ServiceException;
import com.boot.study.response.EnumItem;
import com.boot.study.service.EnumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举数据Service实现类
 * <p>
 * 实现枚举数据查询相关的业务逻辑
 *
 * @author study
 * @since 2024
 */
@Slf4j
@Service
public class EnumServiceImpl implements EnumService {

    /**
     * 获取所有枚举数据
     * <p>
     * 返回系统中所有定义的枚举类型及其选项列表
     *
     * @return 枚举数据Map，key为枚举类型名称，value为枚举选项列表
     */
    @Override
    public Map<String, List<EnumItem>> getAllEnums() {
        Map<String, List<EnumItem>> result = new HashMap<>();

        // 数据源类型
        List<EnumItem> dataSourceTypes = new ArrayList<>();
        for (DataSourceTypeEnum e : DataSourceTypeEnum.values()) {
            dataSourceTypes.add(new EnumItem(e.getCode(), e.getDesc()));
        }
        result.put("dataSourceType", dataSourceTypes);

        // 状态枚举
        List<EnumItem> statusList = new ArrayList<>();
        for (StatusEnum e : StatusEnum.values()) {
            statusList.add(new EnumItem(e.getCode(), e.getDesc()));
        }
        result.put("status", statusList);

        // 质量标志
        List<EnumItem> qualityFlags = new ArrayList<>();
        for (QualityFlagEnum e : QualityFlagEnum.values()) {
            qualityFlags.add(new EnumItem(e.getCode(), e.getDesc()));
        }
        result.put("qualityFlag", qualityFlags);

        // 场景类型
        List<EnumItem> sceneTypes = new ArrayList<>();
        for (SceneTypeEnum e : SceneTypeEnum.values()) {
            sceneTypes.add(new EnumItem(e.getCode(), e.getDesc()));
        }
        result.put("sceneType", sceneTypes);

        // 图表类型
        List<EnumItem> chartTypes = new ArrayList<>();
        for (ChartTypeEnum e : ChartTypeEnum.values()) {
            chartTypes.add(new EnumItem(e.getCode(), e.getDesc()));
        }
        result.put("chartType", chartTypes);

        // 配置类型
        List<EnumItem> configTypes = new ArrayList<>();
        for (ConfigTypeEnum e : ConfigTypeEnum.values()) {
            configTypes.add(new EnumItem(e.getCode(), e.getDesc()));
        }
        result.put("configType", configTypes);

        // 公开标志
        List<EnumItem> publicFlags = new ArrayList<>();
        for (PublicFlagEnum e : PublicFlagEnum.values()) {
            publicFlags.add(new EnumItem(e.getCode(), e.getDesc()));
        }
        result.put("publicFlag", publicFlags);

        return result;
    }

    /**
     * 根据枚举名称获取枚举数据
     * <p>
     * 根据枚举类型名称获取对应的枚举选项列表
     *
     * @param enumName 枚举类型名称
     * @return 枚举选项列表
     * @throws ServiceException 如果枚举类型不存在
     */
    @Override
    public List<EnumItem> getEnumByName(String enumName) {
        List<EnumItem> result = new ArrayList<>();

        switch (enumName) {
            case "dataSourceType":
                for (DataSourceTypeEnum e : DataSourceTypeEnum.values()) {
                    result.add(new EnumItem(e.getCode(), e.getDesc()));
                }
                break;
            case "status":
                for (StatusEnum e : StatusEnum.values()) {
                    result.add(new EnumItem(e.getCode(), e.getDesc()));
                }
                break;
            case "qualityFlag":
                for (QualityFlagEnum e : QualityFlagEnum.values()) {
                    result.add(new EnumItem(e.getCode(), e.getDesc()));
                }
                break;
            case "sceneType":
                for (SceneTypeEnum e : SceneTypeEnum.values()) {
                    result.add(new EnumItem(e.getCode(), e.getDesc()));
                }
                break;
            case "chartType":
                for (ChartTypeEnum e : ChartTypeEnum.values()) {
                    result.add(new EnumItem(e.getCode(), e.getDesc()));
                }
                break;
            case "configType":
                for (ConfigTypeEnum e : ConfigTypeEnum.values()) {
                    result.add(new EnumItem(e.getCode(), e.getDesc()));
                }
                break;
            case "publicFlag":
                for (PublicFlagEnum e : PublicFlagEnum.values()) {
                    result.add(new EnumItem(e.getCode(), e.getDesc()));
                }
                break;
            default:
                throw new ServiceException(400, "不支持的枚举类型: " + enumName);
        }

        return result;
    }
}


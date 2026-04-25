package com.boot.study.service;

import com.boot.study.response.EnumItem;

import java.util.List;
import java.util.Map;

/**
 * 枚举数据Service接口
 * <p>
 * 提供枚举数据查询相关的业务逻辑接口
 *
 * @author study
 * @since 2024
 */
public interface EnumService {

    /**
     * 获取所有枚举数据
     * <p>
     * 返回系统中所有定义的枚举类型及其选项列表
     *
     * @return 枚举数据Map，key为枚举类型名称，value为枚举选项列表
     */
    Map<String, List<EnumItem>> getAllEnums();

    /**
     * 根据枚举名称获取枚举数据
     * <p>
     * 根据枚举类型名称获取对应的枚举选项列表
     *
     * @param enumName 枚举类型名称
     * @return 枚举选项列表
     * @throws com.boot.study.exception.ServiceException 如果枚举类型不存在
     */
    List<EnumItem> getEnumByName(String enumName);
}


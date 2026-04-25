package com.boot.study.enums;

/**
 * 通用的“数据库枚举”泛型接口：
 * 用于描述：数据库字段的存储值 & 对应的业务含义。
 *
 * @param <T> 数据库中实际存储的字段类型（如 Integer、String、Byte 等）
 */
public interface BaseDbEnum<T> {

    /**
     * 数据库存储的值
     */
    T getCode();

    /**
     * 业务含义说明
     */
    String getDesc();
}



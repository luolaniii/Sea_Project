package com.boot.study.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期时间工具类
 * <p>
 * 提供日期时间相关的工具方法，包括字符串解析等功能
 *
 * @author study
 * @since 2024
 */
@Slf4j
public class DateTimeUtil {

    /**
     * 将字符串安全地解析为 LocalDateTime
     * <p>
     * 支持常见的时间格式：
     * - yyyy-MM-dd HH:mm:ss
     * - ISO_LOCAL_DATE_TIME格式
     * 解析失败时返回null，避免直接抛异常，便于业务层处理。
     *
     * @param value 时间字符串
     * @return LocalDateTime对象，解析失败返回null
     */
    public static LocalDateTime parseToLocalDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignore) {
                // 继续尝试下一个格式
            }
        }
        log.warn("无法解析时间字段为 LocalDateTime，原始值: {}", text);
        return null;
    }
}


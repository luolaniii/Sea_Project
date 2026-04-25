package com.boot.study.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统配置类型：
 * STRING-字符串, NUMBER-数字, JSON-JSON对象, BOOLEAN-布尔值
 */
@Getter
@AllArgsConstructor
public enum ConfigTypeEnum implements BaseDbEnum<String> {

    STRING("STRING", "字符串"),
    NUMBER("NUMBER", "数字"),
    JSON("JSON", "JSON 对象"),
    BOOLEAN("BOOLEAN", "布尔值"),
    ;

    @EnumValue
    private final String code;
    private final String desc;

    public static ConfigTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ConfigTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



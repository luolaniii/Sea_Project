package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 观测数据质量标志：
 *  GOOD-良好, QUESTIONABLE-可疑, BAD-坏
 */
@Getter
@AllArgsConstructor
public enum QualityFlagEnum implements BaseDbEnum<String> {

    GOOD("GOOD", "良好"),
    QUESTIONABLE("QUESTIONABLE", "可疑"),
    BAD("BAD", "坏"),
    ;

    private final String code;
    private final String desc;

    public static QualityFlagEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (QualityFlagEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



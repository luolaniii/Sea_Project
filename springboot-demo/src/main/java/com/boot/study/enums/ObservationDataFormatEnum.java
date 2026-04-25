package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 观测数据类型的值格式：
 *  FLOAT-浮点数, INTEGER-整数
 */
@Getter
@AllArgsConstructor
public enum ObservationDataFormatEnum implements BaseDbEnum<String> {

    FLOAT("FLOAT", "浮点数"),
    INTEGER("INTEGER", "整数"),
    ;

    private final String code;
    private final String desc;

    public static ObservationDataFormatEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ObservationDataFormatEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



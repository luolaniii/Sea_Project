package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据同步频率：
 *  HOURLY-每小时, DAILY-每天, WEEKLY-每周
 */
@Getter
@AllArgsConstructor
public enum SyncFrequencyEnum implements BaseDbEnum<String> {

    HOURLY("HOURLY", "每小时"),
    DAILY("DAILY", "每天"),
    WEEKLY("WEEKLY", "每周"),
    ;

    private final String code;
    private final String desc;

    public static SyncFrequencyEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SyncFrequencyEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



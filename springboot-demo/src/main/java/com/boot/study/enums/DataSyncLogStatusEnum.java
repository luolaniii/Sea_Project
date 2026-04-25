package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据同步日志状态：
 *  SUCCESS-成功, FAILED-失败
 */
@Getter
@AllArgsConstructor
public enum DataSyncLogStatusEnum implements BaseDbEnum<String> {

    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    ;

    private final String code;
    private final String desc;

    public static DataSyncLogStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataSyncLogStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



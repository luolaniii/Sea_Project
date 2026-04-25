package com.boot.study.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据同步任务类型：
 *  AUTO-自动同步, MANUAL-手动同步
 */
@Getter
@AllArgsConstructor
public enum DataSyncTaskTypeEnum implements BaseDbEnum<String> {

    AUTO("AUTO", "自动同步"),
    MANUAL("MANUAL", "手动同步"),
    ;

    @EnumValue
    private final String code;
    private final String desc;

    public static DataSyncTaskTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataSyncTaskTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



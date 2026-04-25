package com.boot.study.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据同步任务状态：
 *  RUNNING-运行中, STOPPED-已停止, PAUSED-已暂停
 */
@Getter
@AllArgsConstructor
public enum DataSyncTaskStatusEnum implements BaseDbEnum<String> {

    RUNNING("RUNNING", "运行中"),
    STOPPED("STOPPED", "已停止"),
    PAUSED("PAUSED", "已暂停"),
    ;

    @EnumValue
    private final String code;
    private final String desc;

    public static DataSyncTaskStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataSyncTaskStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



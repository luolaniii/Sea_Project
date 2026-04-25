package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 场景数据关联里的数据类型：
 *  DATA_SOURCE-数据源, DATA-观测数据, FILE-文件
 */
@Getter
@AllArgsConstructor
public enum SceneDataTypeEnum implements BaseDbEnum<String> {

    DATA_SOURCE("DATA_SOURCE", "数据源"),
    DATA("DATA", "观测数据"),
    FILE("FILE", "文件"),
    ;

    private final String code;
    private final String desc;

    public static SceneDataTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SceneDataTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



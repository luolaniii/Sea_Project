package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用“状态”字段枚举：
 *
 * 对应表字段示例：
 *  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用, 1-启用'
 *
 * 这里通过实现 {@link BaseDbEnum} 的泛型接口，将数据库中的值类型（Integer）泛型化。
 */
@Getter
@AllArgsConstructor
public enum StatusEnum implements BaseDbEnum<Integer> {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用"),
    ;

    /**
     * 数据库存储的值，对应 TINYINT(0/1)
     */
    private final Integer code;

    /**
     * 业务含义说明
     */
    private final String desc;

    /**
     * 根据 code 反查枚举
     */
    public static StatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



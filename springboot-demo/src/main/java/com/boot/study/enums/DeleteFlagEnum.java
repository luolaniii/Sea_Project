package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 删除标记：0-未删除, 1-已删除
 */
@Getter
@AllArgsConstructor
public enum DeleteFlagEnum implements BaseDbEnum<Integer> {

    NOT_DELETED(0, "未删除"),
    DELETED(1, "已删除"),
    ;

    private final Integer code;
    private final String desc;

    public static DeleteFlagEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeleteFlagEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



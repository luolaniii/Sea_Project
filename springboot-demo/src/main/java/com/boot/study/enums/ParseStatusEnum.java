package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据文件解析状态：
 *  PENDING-待解析, PROCESSING-解析中, SUCCESS-成功, FAILED-失败
 */
@Getter
@AllArgsConstructor
public enum ParseStatusEnum implements BaseDbEnum<String> {

    PENDING("PENDING", "待解析"),
    PROCESSING("PROCESSING", "解析中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    ;

    private final String code;
    private final String desc;

    public static ParseStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ParseStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



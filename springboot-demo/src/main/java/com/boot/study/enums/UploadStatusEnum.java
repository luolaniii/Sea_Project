package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据文件上传状态：
 *  PENDING-待处理, PROCESSING-处理中, SUCCESS-成功, FAILED-失败
 */
@Getter
@AllArgsConstructor
public enum UploadStatusEnum implements BaseDbEnum<String> {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    ;

    private final String code;
    private final String desc;

    public static UploadStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UploadStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



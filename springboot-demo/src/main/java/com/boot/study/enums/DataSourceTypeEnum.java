package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据源类型：
 *  NOAA-美国海洋大气局, ERDDAP-环境研究数据访问协议, CUSTOM-自定义
 */
@Getter
@AllArgsConstructor
public enum DataSourceTypeEnum implements BaseDbEnum<String> {

    NOAA("NOAA", "美国海洋大气局"),
    ERDDAP("ERDDAP", "环境研究数据访问协议"),
    CUSTOM("CUSTOM", "自定义"),
    ;

    private final String code;
    private final String desc;

    public static DataSourceTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataSourceTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



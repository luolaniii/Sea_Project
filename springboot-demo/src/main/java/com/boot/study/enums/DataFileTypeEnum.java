package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据文件类型：
 *  NETCDF, CSV, JSON, COARDS, WOCE
 *
 * 注意：与原有业务中的 {@link FileTypeEnum} 区分开，这里专指观测数据文件类型。
 */
@Getter
@AllArgsConstructor
public enum DataFileTypeEnum implements BaseDbEnum<String> {

    NETCDF("NETCDF", "NetCDF 文件"),
    CSV("CSV", "CSV 文件"),
    JSON("JSON", "JSON 文件"),
    COARDS("COARDS", "COARDS 格式"),
    WOCE("WOCE", "WOCE 格式"),
    ;

    private final String code;
    private final String desc;

    public static DataFileTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataFileTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



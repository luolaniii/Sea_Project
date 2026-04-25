package com.boot.study.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图表类型：
 *  LINE-折线图, BAR-柱状图, SCATTER-散点图, HEATMAP-热力图, 3D_SURFACE-3D表面图
 */
@Getter
@AllArgsConstructor
public enum ChartTypeEnum implements BaseDbEnum<String> {

    LINE("LINE", "折线图"),
    BAR("BAR", "柱状图"),
    SCATTER("SCATTER", "散点图"),
    HEATMAP("HEATMAP", "热力图"),
    SURFACE_3D("3D_SURFACE", "3D 表面图"),
    ;

    @EnumValue
    private final String code;
    private final String desc;

    public static ChartTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ChartTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



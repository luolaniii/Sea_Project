package com.boot.study.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 可视化场景类型：
 *  3D_OCEAN-3D海洋, 2D_CHART-2D图表, COMPOSITE-复合场景
 *
 * 通过 {@link EnumValue} + MybatisEnumTypeHandler，实现按 code 字段持久化到数据库。
 */
@Getter
@AllArgsConstructor
public enum SceneTypeEnum implements BaseDbEnum<String> {

    OCEAN_3D("3D_OCEAN", "3D 海洋"),
    CHART_2D("2D_CHART", "2D 图表"),
    COMPOSITE("COMPOSITE", "复合场景"),
    ;

    /**
     * 数据库存储的值，比如：3D_OCEAN / 2D_CHART / COMPOSITE
     */
    @EnumValue
    private final String code;

    /**
     * 业务含义描述
     */
    private final String desc;

    public static SceneTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SceneTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



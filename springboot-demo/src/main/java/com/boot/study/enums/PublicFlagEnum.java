package com.boot.study.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 是否公开 / 是否系统配置等 0/1 标志位的通用枚举：
 *  0-否, 1-是
 *
 * 可用于：
 *  - visualization_scene.is_public：0-私有, 1-公开
 *  - chart_config.is_public：0-私有, 1-公开
 *  - system_config.is_system：0-用户配置, 1-系统配置
 *
 * 通过 {@link EnumValue} + MybatisEnumTypeHandler，实现按 code 字段持久化到数据库。
 */
@Getter
@AllArgsConstructor
public enum PublicFlagEnum implements BaseDbEnum<Integer> {

    NO(0, "否"),
    YES(1, "是"),
    ;

    /**
     * 数据库存储的值：0 / 1
     */
    @EnumValue
    private final Integer code;

    /**
     * 业务含义描述
     */
    private final String desc;

    public static PublicFlagEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PublicFlagEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



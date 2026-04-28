package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色：
 *  admin-管理员, user-普通用户, expert-专家用户
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum implements BaseDbEnum<String> {

    ADMIN("admin", "管理员"),
    USER("user", "普通用户"),
    EXPERT("expert", "专家用户"),
    ;

    private final String code;
    private final String desc;

    public static UserRoleEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserRoleEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}



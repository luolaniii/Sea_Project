package com.boot.study.enums;

import com.boot.study.bean.UserInfo;
import com.boot.study.exception.ServiceException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
@AllArgsConstructor
public enum AuthEnum {

    ADMIN(1, "admin", "controller.admin", "管理端接口权限"),
    USER(2, "user", "controller.user", "用户接口权限"),
    EXPERT(3, "expert", "controller.user", "专家用户接口权限");

    private static final String COMMON_PATH = "controller.common";

    private final Integer roleValue;

    /**
     * 权限类型字符
     */
    private final String role;

    /**
     * 权限路径
     */
    private final String path;
    private final String msg;


    public static void authVerify(UserInfo userInfo, String pathName) {
        //通用路径跳出
        if (pathName.contains(COMMON_PATH)) return;

        AuthEnum authEnum = getAuthByRole(userInfo.getRole());
        if (!pathName.contains(authEnum.getPath())) {
            throw new ServiceException("权限错误!");
        }
    }

    public static AuthEnum getAuthByRole(String role) {
        AuthEnum authEnum = Arrays.stream(values()).filter(e -> Objects.equals(e.getRole(), role)).findAny().orElse(null);
        if (Objects.isNull(authEnum)) throw new ServiceException("权限类型错误!");
        return authEnum;
    }
}

package com.boot.study.bean;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.boot.study.enums.AuthEnum;


public class LoginInfo {

    private static final ThreadLocal<UserInfo> LOCAL = new TransmittableThreadLocal<>();

    /**
     * 清除缓存
     */
    public static void clear() {
        LOCAL.remove();
    }

    public static UserInfo getLoginInfo() {
        return LOCAL.get();
    }

    public static void setLoginInfo(UserInfo info) {
        LOCAL.set(info);
    }

    public static Long getUserId() {
        return getLoginInfo().getId();
    }

    public static String getUserRole(){
        return getLoginInfo().getRole();
    }

    public static AuthEnum getAuthEnum() {
        return AuthEnum.getAuthByRole(getUserRole());
    }

}

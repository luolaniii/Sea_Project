package com.boot.study.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultEnum {

    //这里是可以自己定义的，方便与前端交互即可
    SUCCESS(200, "success"),
    HANDLE_SUCCESS(200, "操作成功!"),
    PAY_SUCCESS(200, "支付成功!"),
    FAIL(500, "系统异常"),
    PARAM_ERROR(201, "参数类型错误"),
    NOT_TOKEN(401, "未登录,请重新登录!"),
    TOKEN_PAST(401, "登陆过期,请重新登录!"),
    USER_AUTHORITY_ERROR(402, "用户权限校验失败"),
    PWD_ERROR(1203, "手机号或密码不正确,请检查!"),
    TOKEN_DECODE_ERROR(1204, "token解析错误,请检查!"),
    TOKEN_ERROR(401, "请重新登录!"),
    PAGE_ERROR(1206, "分页数据错误!"),
    DATA_ERROR(1207, "数据异常!"),
    VERIFICATION_CODE_ERROR(1208, "验证码错误!"),
    LOGIN_ERROR(1209, "登录失败"),

    // 通用CRUD操作
    NOT_FOUND(404, "数据不存在"),
    SAVE_FAILED(500, "保存失败"),
    UPDATE_FAILED(500, "更新失败"),
    DELETE_FAILED(500, "删除失败"),
    ;
    private final Integer code;
    private final String msg;
}

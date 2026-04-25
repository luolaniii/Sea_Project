package com.boot.study.bean;

import com.boot.study.enums.ResultEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果封装类
 * <p>
 * 用于统一API接口的返回格式，包含：
 * - code: 响应状态码（200表示成功，其他表示失败）
 * - msg: 响应消息
 * - data: 响应数据（泛型，可以是任意类型）
 *
 * @param <T> 响应数据的类型
 * @author study
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 响应状态码
     * 200: 成功
     * 其他: 失败（具体错误码见ResultEnum）
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 私有构造函数：仅设置code和msg
     *
     * @param code 状态码
     * @param msg  消息
     */
    private Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 私有构造函数：从ResultEnum创建
     *
     * @param resultEnum 结果枚举
     */
    private Result(ResultEnum resultEnum) {
        this.code = resultEnum.getCode();
        this.msg = resultEnum.getMsg();
    }

    /**
     * 私有构造函数：从ResultEnum创建，并设置数据
     *
     * @param resultEnum 结果枚举
     * @param data       响应数据
     */
    private Result(ResultEnum resultEnum, T data) {
        this(resultEnum);
        this.data = data;
    }

    // ==================== 成功响应方法 ====================

    /**
     * 创建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultEnum.SUCCESS, data);
    }

    /**
     * 创建成功响应（无数据）
     *
     * @return Result对象
     */
    public static Result<?> success() {
        return new Result<>(ResultEnum.SUCCESS);
    }

    /**
     * 创建成功响应（自定义code和msg）
     *
     * @param code 状态码
     * @param msg  消息
     * @return Result对象
     */
    public static Result<?> success(int code, String msg) {
        return new Result<>(code, msg);
    }

    /**
     * 创建成功响应（使用ResultEnum）
     *
     * @param resultEnum 结果枚举
     * @return Result对象
     */
    public static Result<?> success(ResultEnum resultEnum) {
        return new Result<>(resultEnum);
    }

    // ==================== 失败响应方法 ====================

    /**
     * 创建失败响应（使用ResultEnum）
     *
     * @param resultEnum 结果枚举
     * @param <T>        数据类型
     * @return Result对象
     */
    public static <T> Result<T> fail(ResultEnum resultEnum) {
        return new Result<>(resultEnum);
    }

    /**
     * 创建失败响应（自定义code和msg）
     *
     * @param code 状态码
     * @param msg  消息
     * @param <T>  数据类型
     * @return Result对象
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg);
    }

    /**
     * 创建失败响应（仅消息，默认code为400）
     *
     * @param msg 消息
     * @return Result对象
     */
    public static Result<?> fail(String msg) {
        return new Result<>(400, msg);
    }

    /**
     * 创建失败响应（使用ResultEnum，带数据）
     *
     * @param resultEnum 结果枚举
     * @param data       响应数据
     * @param <T>        数据类型
     * @return Result对象
     */
    public static <T> Result<T> fail(ResultEnum resultEnum, T data) {
        return new Result<>(resultEnum, data);
    }

    /**
     * 创建失败响应（自定义code、msg和数据）
     *
     * @param code 状态码
     * @param msg  消息
     * @param data 响应数据
     * @param <T>  数据类型
     * @return Result对象
     */
    public static <T> Result<T> fail(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }
}
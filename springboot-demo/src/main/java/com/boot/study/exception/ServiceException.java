package com.boot.study.exception;

import com.boot.study.enums.ResultEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常类
 * <p>
 * 用于处理业务逻辑中的异常情况，继承自RuntimeException。
 * 特点：
 * - 不打印堆栈信息，提升性能
 * - 包含错误码和错误消息
 * - 可通过ResultEnum快速创建
 *
 * @author study
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceException extends RuntimeException {

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String msg;

    /**
     * 默认构造函数
     * 默认错误消息为"系统异常"
     */
    public ServiceException() {
        super();
        this.msg = "系统异常";
    }

    /**
     * 构造函数：仅设置错误消息
     *
     * @param message 错误消息
     */
    public ServiceException(String message) {
        super(message);
        this.msg = message;
    }

    /**
     * 构造函数：设置错误码和错误消息
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
        this.msg = message;
    }

    /**
     * 构造函数：从ResultEnum创建异常
     *
     * @param resultEnum 结果枚举
     */
    public ServiceException(ResultEnum resultEnum) {
        this(resultEnum.getCode(), resultEnum.getMsg());
    }

    /**
     * 重写fillInStackTrace方法，不填充堆栈信息
     * <p>
     * 性能优化：自定义异常通常不需要堆栈信息，避免性能损耗
     *
     * @return 当前异常对象
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    /**
     * 重写getMessage方法，返回自定义的错误消息
     *
     * @return 错误消息
     */
    @Override
    public String getMessage() {
        return this.msg;
    }
}

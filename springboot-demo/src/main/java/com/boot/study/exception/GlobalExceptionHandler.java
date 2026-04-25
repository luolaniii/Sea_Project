package com.boot.study.exception;

import com.boot.study.bean.Result;
import com.boot.study.enums.ResultEnum;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理应用程序中抛出的各种异常，并返回统一的响应格式。
 * 使用@RestControllerAdvice注解，可以捕获所有Controller中抛出的异常。
 *
 * @author study
 * @since 2024
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理未知异常
     * <p>
     * 捕获所有未被其他异常处理器处理的异常
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<?> unknownException(Exception e, HttpServletRequest request) {
        log.error("RequestURI:{}; Catch Exception:{}", request.getRequestURI(), e.getMessage(), e);
        return Result.fail(ResultEnum.FAIL);
    }

    /**
     * 处理HTTP消息不可读异常
     * <p>
     * 通常发生在请求体格式错误或无法解析时
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> httpMessageNotReadableException(Exception e, HttpServletRequest request) {
        log.error("RequestURI:{}; Catch Exception:{}", request.getRequestURI(), e.getMessage(), e);
        return Result.fail(ResultEnum.PARAM_ERROR);
    }

    /**
     * 处理非法参数异常
     * <p>
     * 通常发生在参数验证失败时
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> illegalArgumentException(Exception e, HttpServletRequest request) {
        log.error("RequestURI:{}; Catch Exception:{}", request.getRequestURI(), e.getMessage(), e);
        return Result.fail(500, e.getMessage());
    }

    /**
     * 处理方法参数校验异常
     * <p>
     * 通常发生在使用@Valid注解进行参数校验时，校验失败会抛出此异常
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> methodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        // 提取所有字段校验错误信息，用逗号连接
        String errorInfo = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(","));
        log.error("RequestURI:{}; Catch Exception Msg:{}", request.getRequestURI(), errorInfo);
        return Result.fail(400, errorInfo);
    }

    /**
     * 处理HTTP请求方法不支持异常
     * <p>
     * 通常发生在请求的HTTP方法（GET、POST等）与Controller中定义的方法不匹配时
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> httpRequestMethodNotSupportedException(Exception e, HttpServletRequest request) {
        log.error("RequestURI:{}; Catch Exception:{}", request.getRequestURI(), e.getMessage(), e);
        return Result.fail(500, e.getMessage());
    }

    /**
     * 处理业务异常
     * <p>
     * 处理Service层抛出的业务异常
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> serviceException(ServiceException e, HttpServletRequest request) {
        log.error("RequestURI:{}; Catch Exception:{}", request.getRequestURI(), e.getMessage());
        // 如果异常中没有设置错误码，使用默认的500
        if (e.getCode() == null) {
            return Result.fail(500, e.getMessage());
        }
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理带堆栈信息的业务异常
     * <p>
     * 处理需要打印堆栈信息的业务异常
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(StackServiceException.class)
    public Result<?> stackServiceException(StackServiceException e, HttpServletRequest request) {
        log.error("RequestURI:{}; Catch Exception:{}", request.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理MyBatis系统异常
     * <p>
     * 处理MyBatis框架抛出的异常，如果根原因是ServiceException，则提取其错误信息
     *
     * @param e       异常对象
     * @param request HTTP请求对象
     * @return 统一响应结果
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public Result<?> myBatisSystemException(MyBatisSystemException e, HttpServletRequest request) {
        log.error("RequestURI:{}; Catch Exception:{}", request.getRequestURI(), e.getMessage(), e);
        Throwable cause = e.getRootCause();
        // 如果根原因是ServiceException，返回其错误信息
        if (Objects.nonNull(cause) && cause instanceof ServiceException) {
            return Result.fail(500, cause.getMessage());
        }
        return Result.fail(ResultEnum.FAIL);
    }
}

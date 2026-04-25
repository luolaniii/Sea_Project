package com.boot.study.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 异常工具类
 * <p>
 * 提供异常处理的工具方法，如获取异常堆栈信息等
 *
 * @author study
 * @since 2024
 */
@Slf4j
public class ExceptionUtil {

    /**
     * 获取异常的堆栈信息字符串
     * <p>
     * 将异常的堆栈信息转换为字符串，用于日志记录或错误报告
     *
     * @param e 异常对象
     * @return 异常堆栈信息字符串
     */
    public static String getExceptionStackString(Exception e) {
        if (e == null) {
            return "";
        }
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            return sw.toString();
        } catch (Exception ex) {
            log.error("获取异常堆栈信息失败", ex);
            return e.getMessage() != null ? e.getMessage() : "";
        }
    }
}

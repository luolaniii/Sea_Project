package com.boot.study.aop;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class LogAspect {

    /**
     * 申明一个切点 里面是 execution表达式
     */
    @Pointcut("execution(public * com.boot.study.controller.*.*.*(..))&& !@annotation(com.boot.study.annotation.NotLog)")
    private void controllerAspect() {
    }

    /**
     * 请求method前打印内容
     *
     * @param joinPoint 请求数据
     */
    @Before(value = "controllerAspect()")
    public void enterLogBefore(JoinPoint joinPoint) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert requestAttributes != null;
        HttpServletRequest request = requestAttributes.getRequest();
        log.info("-----------------请求内容--------------");
        try {
            String ip = this.getRequestIp(request);
            // 打印请求内容
            log.info("客户端IP：{}", ip);
            log.info("请求方式：{},地址{}", request.getMethod(), request.getRequestURL().toString());
            log.info("请求类型：{}", request.getContentType());
            Object[] pointArgs = joinPoint.getArgs();
            if (request.getMethod() != null && request.getMethod().contains("POST")) {
                if (pointArgs.length > 0) {
                    log.info("post请求类方法参数：\n{}", this.toJson(pointArgs[0]));
                }
            } else if (request.getMethod() != null && request.getMethod().contains("GET")) {
                if (pointArgs.length > 0) {
                    StringBuilder str = new StringBuilder();
                    for (Object pointArg : pointArgs) {
                        str.append(pointArg).append(" ");
                    }
                    log.info("get请求类方法参数:{}", str);
                }
            }
        } catch (Exception e) {
            log.error("### LogAop.class methodBefore() ### ERROR：{}", e.getMessage(), e);
        }
    }

    /**
     * 在方法执行完结后打印返回内容
     */
    @AfterReturning(returning = "o", pointcut = "controllerAspect()")
    public void returnLogAfter(Object o) {
        log.info("--------------返回内容----------------");
        try {
            String json = this.toJson(o);
            if (json.length() > 50000) return;
            log.info("Response内容：\n{}", json);
        } catch (Exception e) {
            log.error("### LogAop.class methodAfterReturing() ### ERROR：{}", e.getMessage(), e);
        }
    }

    /**
     * 格式化json
     *
     * @param object 数据
     * @return String
     */
    private String toJson(Object object) {
        return JSON.toJSONString(object);
    }

    /**
     * 获取请求方的IP地址
     *
     * @param request 当前请求
     * @return String
     */
    private String getRequestIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        String str = "unknown";
        if (ip == null || ip.length() == 0 || str.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || str.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || str.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || str.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || str.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || str.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
package com.boot.study.response;

import com.boot.study.bean.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice("com.boot")
public class MyResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * response包装
     */
    @Override
    public Object beforeBodyWrite(Object data, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (data instanceof Result) {
            return data;
        }

        if (data instanceof CharSequence) {
            response.getHeaders().set("Content-Type", "application/json;charset=UTF-8");
            response.getHeaders().remove("Content-Length");
//            return JSON.toJSONString(Result.success(data));
            //开放平台要求返回success 字符串不再包裹进Result
            return data;
        }

        return Result.success(data);
    }
}

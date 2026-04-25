package com.boot.study.config;

import cn.hutool.core.util.StrUtil;

import com.boot.study.annotation.PassToken;

import com.boot.study.bean.LoginInfo;
import com.boot.study.bean.TokenInfo;
import com.boot.study.bean.UserInfo;
import com.boot.study.consts.BaseConst;
import com.boot.study.enums.AuthEnum;
import com.boot.study.enums.ResultEnum;
import com.boot.study.exception.ServiceException;
import com.boot.study.utils.RedisUtil;
import com.boot.study.utils.TokenUtil;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    public static final String AUTHORIZATION = "Authorization";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) {

        String token = request.getHeader(AUTHORIZATION);

        // 如果不是映射到方法直接通过
        if (!(object instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) object;
        Method method = handlerMethod.getMethod();

        PassToken passTokenBean = AnnotationUtils.findAnnotation(handlerMethod.getBean().getClass(), PassToken.class);
        //检查是否有passToken注释，有则跳过认证
        if (passTokenBean != null) return true;
        if (method.isAnnotationPresent(PassToken.class)) {
            PassToken passToken = method.getAnnotation(PassToken.class);
            if (passToken.required()) {
                return true;
            }
        }
        if (StrUtil.isBlank(token)) {
            throw new ServiceException(ResultEnum.NOT_TOKEN);
        }
        
        TokenInfo tokenInfo = TokenUtil.parse(token);
        UserInfo userInfo = RedisUtil.getObject(BaseConst.TOKEN + tokenInfo.getTokenPath(), UserInfo.class);
        if (userInfo == null) {
            throw new ServiceException(ResultEnum.TOKEN_PAST);
        }

        // 过滤器中权限验证
        String pathName = handlerMethod.getBean().getClass().getName();
        AuthEnum.authVerify(userInfo, pathName);

        LoginInfo.setLoginInfo(userInfo);
        return verifyToken(token, userInfo);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView modelAndView) {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) {
        LoginInfo.clear();
    }

    /**
     * 验证Token是否有效
     *
     * @param reqToken  请求中的Token
     * @param userInfo  用户信息
     * @return 验证结果
     */
    private boolean verifyToken(String reqToken, UserInfo userInfo) {
        if (!reqToken.equals(userInfo.getToken())) {
            throw new ServiceException(ResultEnum.TOKEN_ERROR);
        }
        return true;
    }
}

package com.boot.study.config;

import com.boot.study.consts.WebPathConst;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * <p>
 * 配置拦截器、静态资源处理器等
 *
 * @author study
 * @since 2024
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor interceptor;

    /**
     * 添加拦截器
     * <p>
     * 配置JWT拦截器，排除Swagger、静态资源等路径
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        WebPathConst.SWAGGER_UI_HTML,
                        WebPathConst.DOC_HTML,
                        WebPathConst.V3_API_DOCS,
                        WebPathConst.FAVICON_ICO,
                        WebPathConst.ERROR,
                        WebPathConst.WEBJARS,
                        WebPathConst.SWAGGER_RESOURCES,
                        // 上传文件通过 <a href> 打开时不带 Token，需放行静态映射
                        "/uploads/**"
                );
    }

    /**
     * 添加静态资源处理器
     * <p>
     * 配置静态资源的访问路径和位置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 通用静态资源
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        WebPathConst.CLASSPATH_META_INF_RESOURCES,
                        WebPathConst.CLASSPATH_STATIC
                )
                .setCachePeriod(0);

        // Swagger UI
        registry.addResourceHandler(WebPathConst.SWAGGER_UI_HTML)
                .addResourceLocations(WebPathConst.CLASSPATH_META_INF_RESOURCES);

        // Knife4j文档
        registry.addResourceHandler(WebPathConst.DOC_HTML)
                .addResourceLocations(WebPathConst.CLASSPATH_META_INF_RESOURCES);

        // Webjars资源
        registry.addResourceHandler(WebPathConst.WEBJARS)
                .addResourceLocations(WebPathConst.CLASSPATH_META_INF_RESOURCES + "webjars/");

        // 模板资源
        registry.addResourceHandler(WebPathConst.TEMPLATES)
                .addResourceLocations("classpath:/templates/");

        // 用户上传文件映射（论坛附件等）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/");
    }
}

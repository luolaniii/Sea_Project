package com.boot.study.config;

import com.google.common.collect.Lists;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;


@Configuration
@EnableSwagger2
@Profile("dev") // 仅在开发或测试环境启用
public class SwaggerConfig {

    private static final String AUTHORIZATION = "Authorization";
    private static final String APPID = "APPID";

    @Bean
    @ConditionalOnProperty(name = "swagger.enabled", havingValue = "true")
    public Docket createAdminRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("管理端")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.boot.study.controller.admin"))
                .paths(PathSelectors.any())
                .build()
                //增加全局token
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    @Bean
    @ConditionalOnProperty(name = "swagger.enabled", havingValue = "true")
    public Docket createInnerRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("教师端")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.boot.study.controller.teacher"))
                .paths(PathSelectors.any())
                .build()
                //增加全局token
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    @Bean
    @ConditionalOnProperty(name = "swagger.enabled", havingValue = "true")
    public Docket createOutApi() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("学生端")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.boot.study.controller.studnet"))
                .paths(PathSelectors.any())
                .build()
                //增加全局token
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    @Bean
    @ConditionalOnProperty(name = "swagger.enabled", havingValue = "true")
    public Docket createCommonRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .groupName("多端共用")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.boot.study.controller.common"))
                .paths(PathSelectors.any())
                .build()
                //增加全局token
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                //页面标题
                .title("API接口文档")
                //网站描述
                .description("阳帆918加盟平台")
                //版本
                .version("1.0")
                //条款地址
                .termsOfServiceUrl("https://yangfan918.top")
                //联系人
                .contact(new Contact("阳帆918", "https://yangfan918.top", ""))
                //协议
                .license("The Apache License")
                //协议url
                .licenseUrl("test")
                .build();
    }


    private List<SecurityScheme> securitySchemes() {
        return Lists.newArrayList(new ApiKey(AUTHORIZATION, AUTHORIZATION, "header"), new ApiKey(APPID, APPID, "header"));
    }


    List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> securityReferences = new ArrayList<>();
        securityReferences.add(new SecurityReference(AUTHORIZATION, authorizationScopes));
        securityReferences.add(new SecurityReference(APPID, authorizationScopes));
        return securityReferences;
    }

    private List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(
                SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        .build());
        return securityContexts;
    }

}

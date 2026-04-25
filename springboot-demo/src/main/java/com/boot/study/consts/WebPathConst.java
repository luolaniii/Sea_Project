package com.boot.study.consts;

/**
 * Web路径常量类
 * <p>
 * 统一管理Web相关的路径配置，避免硬编码
 *
 * @author study
 * @since 2024
 */
public class WebPathConst {

    /**
     * Swagger相关路径
     */
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String DOC_HTML = "/doc.html";
    public static final String V3_API_DOCS = "/v3/*";
    public static final String WEBJARS = "/webjars/**";
    public static final String SWAGGER_RESOURCES = "/swagger-resources/**";

    /**
     * 静态资源路径
     */
    public static final String FAVICON_ICO = "/favicon.ico";
    public static final String ERROR = "/error";
    public static final String TEMPLATES = "/templates/**";

    /**
     * 资源位置
     */
    public static final String CLASSPATH_META_INF_RESOURCES = "classpath:/META-INF/resources/";
    public static final String CLASSPATH_STATIC = "classpath:/static/";

    /**
     * 私有构造函数，防止实例化
     */
    private WebPathConst() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}


package com.boot.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.mybatis.spring.annotation.MapperScan;

/**
 * Spring Boot 应用程序启动类
 * <p>
 * 功能说明：
 * - @EnableScheduling: 启用定时任务支持
 * - @EnableTransactionManagement: 启用事务管理
 * - @MapperScan: 扫描MyBatis Mapper接口
 * - @SpringBootApplication: Spring Boot应用主注解
 *
 * @author study
 * @since 2024
 */
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.boot.study.dao")
@SpringBootApplication(scanBasePackages = "com.boot")
public class StudyApplication {

    /**
     * 应用程序入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(StudyApplication.class, args);
    }
}

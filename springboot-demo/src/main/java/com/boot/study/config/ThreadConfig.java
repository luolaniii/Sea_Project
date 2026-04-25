package com.boot.study.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class ThreadConfig implements AsyncConfigurer {

    @Bean
    @Primary
    @Override
    public Executor getAsyncExecutor() {
        //根据ThreadPoolTaskExecutor 创建建线程池
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //为线程设置初始的线程数量 5条线程
        executor.setCorePoolSize(10);
        //为线程设置最大的线程数量 10条线程
        executor.setMaxPoolSize(20);
        //为任务队列设置最大 任务数量
        executor.setQueueCapacity(50);
        //非核心线程闲置下来不干活最多存活时间
        executor.setKeepAliveSeconds(60);
        //设置 线程前缀
        executor.setThreadNamePrefix("async-");
        //线程池的饱和策略 我这里设置的是 CallerRunsPolicy 也就是由用调用者所在的线程来执行任务 共有四种
        //AbortPolicy：直接抛出异常，这是默认策略；
        //CallerRunsPolicy：用调用者所在的线程来执行任务；
        //DiscardOldestPolicy：丢弃阻塞队列中靠最前的任务，并执行当前任务；
        //DiscardPolicy：直接丢弃任务；
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //设置在关闭线程池时是否等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //设置等待终止的秒数
        executor.setAwaitTerminationSeconds(60);
        //初始化
        executor.initialize();
        //返回设置完成的线程池
        return TtlExecutors.getTtlExecutor(executor);
    }
}


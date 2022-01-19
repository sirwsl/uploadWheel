package com.wslhome.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 线程池配置
 *
 * @author sirwsl
 * @date 2022/01/11-11:14
 **/
@Configuration
@EnableAsync
public class SpringAsyncConfig {
    /**
     * 线程池参数根据minIO设置，如果开启线程太多会被MinIO拒绝
     * @return ：
     */
    @Bean("minIOUploadTreadPool")
    public ThreadPoolTaskExecutor  asyncServiceExecutorForMinIo() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数，采用IO密集 h/(1-拥塞)
        executor.setCorePoolSize(6);
        // 设置最大线程数,由于minIO连接数量有限，此处尽力设计大点
        executor.setMaxPoolSize(500);
        // 设置线程活跃时间（秒）
        executor.setKeepAliveSeconds(30);
        // 设置默认线程名称
        executor.setThreadNamePrefix("minio-upload-task-");
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //执行初始化
        executor.initialize();
        return executor;
    }

    /**
     * oss async
     * @return
     */
    @Bean("ossUploadTreadPool")
    public ThreadPoolTaskExecutor  asyncServiceExecutorForOss() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数，采用IO密集 h/(1-拥塞)
        executor.setCorePoolSize(8);
        // 设置最大线程数,由于minIO连接数量有限，此处尽力设计大点
        executor.setMaxPoolSize(120);
        // 设置线程活跃时间（秒）
        executor.setKeepAliveSeconds(30);
        // 设置默认线程名称
        executor.setThreadNamePrefix("ossUploadTask-");
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //执行初始化
        executor.initialize();
        return executor;
    }

}

package com.risk.controller.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * SpringBoot启动器
 *
 * @Author ZT
 * @create 2018-08-27
 */
// @SpringBootApplication 相当于@Configuration,@EnableAutoConfiguration,@ComponentScan
@SpringBootApplication(scanBasePackages = {"com.risk.controller.service"})
@EnableTransactionManagement// 加下事物处理
@MapperScan("com.risk.controller.service.dao")// mybatis框架中的dao扫描
@EnableAsync // 启动异步调用
@EnableScheduling
public class SpringBootStart extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootStart.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SpringBootStart.class);
    }

    /**
     * 自定义异步线程池
     * @return
     */
    @Bean
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(30);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(1000000);
        executor.setKeepAliveSeconds(300);
        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}

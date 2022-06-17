package com.le.yygh.order.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


//@Mapper
@Configuration
@MapperScan("com.le.yygh.order.mapper")
public class OrderConfig {
    /**
     * 分页插件,没有的话，mybatisplus分页不起作用5.3
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }
}

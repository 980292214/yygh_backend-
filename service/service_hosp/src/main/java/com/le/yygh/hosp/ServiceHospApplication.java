package com.le.yygh.hosp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author 乐
 * @version 1.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.le")
@EnableDiscoveryClient//被 nacos 找到;服务注册
@EnableFeignClients(basePackages = "com.le")//找到 FeignClients
public class ServiceHospApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceHospApplication.class,args);
    }
}

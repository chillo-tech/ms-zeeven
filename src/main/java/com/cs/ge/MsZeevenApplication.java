package com.cs.ge;

import io.mongock.runner.springboot.EnableMongock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableAsync
@EnableMongock
@EnableScheduling
@EnableFeignClients
@SpringBootApplication
public class MsZeevenApplication {
    public static void main(final String[] args) {
        SpringApplication.run(MsZeevenApplication.class, args);
    }

}

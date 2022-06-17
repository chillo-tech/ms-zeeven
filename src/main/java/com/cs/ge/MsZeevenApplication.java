package com.cs.ge;

import io.mongock.runner.springboot.EnableMongock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@EnableMongock
@SpringBootApplication
public class MsZeevenApplication {
    public static void main(final String[] args) {
        SpringApplication.run(MsZeevenApplication.class, args);
    }

}

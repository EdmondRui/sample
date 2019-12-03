package com.moon.sample;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@EnableDubbo
public class ManageServiceStarter {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ManageServiceStarter.class).web(WebApplicationType.NONE).run(args);
    }
}

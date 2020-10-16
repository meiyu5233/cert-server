package com.tmxbase.certserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource({"license-config.properties"}) //加载额外的配置
public class CertServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertServerApplication.class, args);
    }

}

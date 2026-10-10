package com.smartpay.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartPaymentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartPaymentPlatformApplication.class, args);
    }
}

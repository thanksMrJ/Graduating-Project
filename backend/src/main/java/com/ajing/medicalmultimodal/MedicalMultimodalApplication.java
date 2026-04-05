package com.ajing.medicalmultimodal;

import com.ajing.medicalmultimodal.oss.AliOssProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AliOssProperties.class)
public class MedicalMultimodalApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalMultimodalApplication.class, args);
    }

}

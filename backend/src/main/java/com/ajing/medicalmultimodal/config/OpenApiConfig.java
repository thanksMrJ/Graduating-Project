package com.ajing.medicalmultimodal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medicalMultimodalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("面向肝胆肿瘤的多模态推理与疾病早筛 — API")
                        .description("与 Vue 前端、开题报告中的多模态采集 / 任务编排 / 推理流程对齐的 REST 约定说明见 docs/API.md；此处为在线文档入口。")
                        .version("v1")
                        .contact(new Contact().name("medical-multimodal"))
                        .license(new License().name("Proprietary").url("about:blank")));
    }
}

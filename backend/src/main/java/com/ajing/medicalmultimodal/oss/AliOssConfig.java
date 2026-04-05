package com.ajing.medicalmultimodal.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(prefix = "alioss", name = "enabled", havingValue = "true")
public class AliOssConfig {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(AliOssProperties properties) {
        if (!StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())) {
            throw new IllegalStateException(
                    "alioss.enabled=true 时必须配置 alioss.access-key-id 与 alioss.access-key-secret（或环境变量 ALIYUN_OSS_ACCESS_KEY_ID / ALIYUN_OSS_ACCESS_KEY_SECRET）");
        }
        String endpoint = normalizeEndpoint(properties.getEndpoint());
        return new OSSClientBuilder().build(
                endpoint,
                properties.getAccessKeyId().trim(),
                properties.getAccessKeySecret().trim());
    }

    static String normalizeEndpoint(String endpoint) {
        String e = endpoint == null ? "" : endpoint.trim();
        if (!StringUtils.hasText(e)) {
            e = "https://oss-cn-shanghai.aliyuncs.com";
        } else if (!e.startsWith("http://") && !e.startsWith("https://")) {
            e = "https://" + e;
        }
        return e;
    }
}

package com.ajing.medicalmultimodal.config;

import com.aliyun.oss.OSS;
import com.ajing.medicalmultimodal.oss.AliOssProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时打印当前文件存储方式，避免误以为已开 OSS 实际仍写本地。
 */
@Component
@Order(0)
public class StorageStartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageStartupLogger.class);

    private final AliOssProperties alioss;
    private final String uploadDir;

    @Autowired(required = false)
    private OSS ossClient;

    public StorageStartupLogger(AliOssProperties alioss, @Value("${app.upload.dir:./data/uploads}") String uploadDir) {
        this.alioss = alioss;
        this.uploadDir = uploadDir;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (alioss.isEnabled() && ossClient != null) {
            log.info(
                    "[文件存储] 阿里云 OSS 已启用 | endpoint={} | bucket={} | object-prefix={}",
                    alioss.getEndpoint(),
                    alioss.getBucketName(),
                    alioss.getObjectPrefix());
        } else {
            log.warn(
                    "[文件存储] 未使用 OSS，文件写入本机目录: {} （若要用 OSS，请设置 alioss.enabled=true 并配置 AK/SK 后重启）",
                    uploadDir);
        }
    }
}

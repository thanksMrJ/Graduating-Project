package com.ajing.medicalmultimodal.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.ajing.medicalmultimodal.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@ConditionalOnBean(OSS.class)
public class AliOssStorageService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final OSS ossClient;
    private final AliOssProperties properties;

    public AliOssStorageService(OSS ossClient, AliOssProperties properties) {
        this.ossClient = ossClient;
        this.properties = properties;
    }

    /**
     * 上传用户文件，返回对象键与公网访问 URL（若 Bucket 为私有读，URL 需在控制台生成签名或走服务端代理）。
     */
    public UploadResult uploadUserFile(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        String original = sanitizeOriginalName(file.getOriginalFilename());
        String prefix = normalizePrefix(properties.getObjectPrefix());
        String extra = StringUtils.hasText(subDir) ? sanitizeSubDir(subDir) + "/" : "";
        String day = LocalDate.now().format(DAY);
        String objectKey = prefix + extra + day + "/" + UUID.randomUUID() + "-" + original;

        ObjectMetadata meta = new ObjectMetadata();
        if (file.getContentType() != null) {
            meta.setContentType(file.getContentType());
        }
        long size = file.getSize();
        if (size > 0) {
            meta.setContentLength(size);
        }

        try (InputStream in = file.getInputStream()) {
            PutObjectRequest req = new PutObjectRequest(
                    properties.getBucketName(),
                    objectKey,
                    in,
                    meta);
            ossClient.putObject(req);
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败");
        }

        return new UploadResult(objectKey, buildPublicUrl(objectKey));
    }

    private String buildPublicUrl(String objectKey) {
        String endpoint = AliOssConfig.normalizeEndpoint(properties.getEndpoint());
        URI uri = URI.create(endpoint);
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            return "";
        }
        String bucket = properties.getBucketName();
        return "https://" + bucket + "." + host + "/" + objectKey;
    }

    private static String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "uploads/";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private static String sanitizeOriginalName(String name) {
        if (!StringUtils.hasText(name)) {
            return "file";
        }
        String n = name.replace("\\", "/");
        int i = n.lastIndexOf('/');
        n = i >= 0 ? n.substring(i + 1) : n;
        if (n.contains("..") || n.contains("/")) {
            throw new BusinessException("非法文件名");
        }
        if (n.length() > 200) {
            n = n.substring(n.length() - 200);
        }
        return n.isEmpty() ? "file" : n;
    }

    private static String sanitizeSubDir(String subDir) {
        String s = subDir.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        if (s.contains("..") || s.startsWith("/")) {
            throw new BusinessException("非法目录参数");
        }
        return s.replaceAll("/+", "/").replaceAll("^/|/$", "");
    }

    public record UploadResult(String objectKey, String url) {
    }
}

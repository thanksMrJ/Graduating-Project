package com.ajing.medicalmultimodal.storage;

import com.aliyun.oss.OSS;
import com.ajing.medicalmultimodal.oss.AliOssStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnBean(OSS.class)
public class AliyunOssFileStorage implements FileStorageService {

    private final AliOssStorageService delegate;

    public AliyunOssFileStorage(AliOssStorageService delegate) {
        this.delegate = delegate;
    }

    @Override
    public UploadResult store(MultipartFile file, String folder, Long userId) {
        String sub = "u" + userId;
        if (StringUtils.hasText(folder)) {
            sub = sub + "/" + folder.trim();
        }
        AliOssStorageService.UploadResult r = delegate.uploadUserFile(file, sub);
        return new UploadResult(r.objectKey(), r.url());
    }
}

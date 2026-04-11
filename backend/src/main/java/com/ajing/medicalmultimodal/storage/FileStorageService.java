package com.ajing.medicalmultimodal.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {

    UploadResult store(MultipartFile file, String folder, Long userId) throws IOException;

    record UploadResult(String objectKey, String url) {
    }
}

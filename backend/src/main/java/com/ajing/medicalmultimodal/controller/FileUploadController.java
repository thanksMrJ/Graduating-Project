package com.ajing.medicalmultimodal.controller;

import com.ajing.medicalmultimodal.common.ApiResponse;
import com.ajing.medicalmultimodal.common.BusinessException;
import com.ajing.medicalmultimodal.security.JwtKind;
import com.ajing.medicalmultimodal.security.JwtPrincipal;
import com.ajing.medicalmultimodal.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/uploads")
@Tag(name = "Uploads", description = "文件上传（OSS 或本地磁盘，见配置）")
public class FileUploadController {

    private final FileStorageService storageService;

    public FileUploadController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/file")
    @Operation(summary = "上传文件", description = "multipart 字段名 file；需用户登录")
    public ApiResponse<Map<String, String>> upload(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) throws IOException {
        if (principal == null || principal.kind() != JwtKind.USER) {
            throw new BusinessException(403, "仅登录用户可上传文件");
        }
        FileStorageService.UploadResult r = storageService.store(file, folder, principal.id());
        Map<String, String> data = new LinkedHashMap<>();
        data.put("fileId", r.objectKey());
        data.put("objectKey", r.objectKey());
        data.put("url", StringUtils.hasText(r.url()) ? r.url() : "");
        return ApiResponse.ok(data);
    }
}

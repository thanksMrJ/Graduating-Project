package com.ajing.medicalmultimodal.controller;

import com.aliyun.oss.OSS;
import com.ajing.medicalmultimodal.common.ApiResponse;
import com.ajing.medicalmultimodal.oss.AliOssStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/uploads")
@ConditionalOnBean(OSS.class)
@Tag(name = "Uploads", description = "文件上传到阿里云 OSS（需 alioss.enabled=true）")
public class FileUploadController {

    private final AliOssStorageService storageService;

    public FileUploadController(AliOssStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * 通用单文件上传；与 docs/API.md 中 multipart 上传约定一致。
     *
     * @param file   表单字段名 file
     * @param folder 可选子目录（如 imaging、document），会拼在 object-prefix 之后
     */
    @PostMapping("/file")
    @Operation(summary = "上传文件到 OSS", description = "multipart 字段名：file；返回 fileId（当前等于 objectKey）、objectKey、url")
    public ApiResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {
        AliOssStorageService.UploadResult r = storageService.uploadUserFile(file, folder);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("fileId", r.objectKey());
        data.put("objectKey", r.objectKey());
        data.put("url", StringUtils.hasText(r.url()) ? r.url() : "");
        return ApiResponse.ok(data);
    }
}

package com.ajing.medicalmultimodal.controller;

import com.ajing.medicalmultimodal.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "服务健康检查")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "用于部署与联调确认服务可用")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "medical-multimodal");
        body.put("timestamp", Instant.now().toString());
        return ApiResponse.ok(body);
    }
}

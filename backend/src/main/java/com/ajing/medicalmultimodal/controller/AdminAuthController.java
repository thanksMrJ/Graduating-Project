package com.ajing.medicalmultimodal.controller;

import com.ajing.medicalmultimodal.common.ApiResponse;
import com.ajing.medicalmultimodal.dto.AdminLoginRequest;
import com.ajing.medicalmultimodal.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "AdminAuth", description = "管理员登录")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest req) {
        return ApiResponse.ok(adminAuthService.login(req));
    }
}

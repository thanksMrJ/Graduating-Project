package com.ajing.medicalmultimodal.controller;

import com.ajing.medicalmultimodal.common.ApiResponse;
import com.ajing.medicalmultimodal.dto.LoginRequest;
import com.ajing.medicalmultimodal.dto.RegisterRequest;
import com.ajing.medicalmultimodal.dto.ResetPasswordRequest;
import com.ajing.medicalmultimodal.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "用户注册与登录")
public class AuthController {

    private final UserAuthService userAuthService;

    public AuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(userAuthService.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(userAuthService.login(req));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码")
    public ApiResponse<Map<String, Object>> reset(@Valid @RequestBody ResetPasswordRequest req) {
        return ApiResponse.ok(userAuthService.resetPassword(req));
    }
}
